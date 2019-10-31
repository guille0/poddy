package com.guille.poddy.services;

import java.io.IOException;

import android.media.*;
import android.os.*;
import android.content.*;
import android.telephony.*;
import android.graphics.*;

import java.lang.Math;
import android.media.session.*;
import android.support.v4.media.session.*;
import android.app.*;
import androidx.core.app.NotificationCompat;

import com.guille.poddy.R;
import com.guille.poddy.database.*;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.guille.poddy.Broadcast;


public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    public static final int PAUSED = 0;
    public static final int PLAYING = 1;

    private static final int UI_REFRESH_DELAY = 500;

    private final Handler handlerRefreshUI = new Handler();

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
    private Bitmap episodeImage;

    // MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

    // Notification ID
    private static final int NOTIFICATION_ID = 101;

    // Episode that's being played, its podcast and file
    private Episode episode;
    private Podcast podcast;
    private String mediaFile;

    // Save the position for when we have to pause
    private int resumePosition = 0;
    // Seconds we skip on pressing fastforward button
    private int fastForwardSeconds = 10;

    // For handling incoming phone calls
    private boolean ongoingCall = false;
    private PhoneStateListener phoneStateListener;
    private TelephonyManager telephonyManager;

    @Override
    public void onCreate() {
        super.onCreate();
        callStateListener();

        registerReceivers();
        registerUIRefresher();
    }

    private void initMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        //Set up MediaPlayer event listeners
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        //Reset so that the MediaPlayer is not pointing to another data source
        mediaPlayer.reset();

        try {
            // Set the data source to the mediaFile location
            mediaPlayer.setDataSource(mediaFile);
        } catch (IOException e) {
            e.printStackTrace();
            stopSelf();
        }

        mediaPlayer.prepareAsync();

        buildNotification(PLAYING);
    }

    // BASIC CONTROLS

    private void playMedia() {
        buildNotification(PLAYING);
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    private void stopMedia(Boolean savePosition) {
        removeNotification();
        if (mediaPlayer == null) return;

        if (savePosition)
            saveEpisodePosition();
        else
            resetEpisodePosition();

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
    }
    private void stopMedia() {
        stopMedia(true);
    }

    private void pauseMedia() {
        buildNotification(PAUSED);
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }

    private void resumeMedia() {
        buildNotification(PLAYING);
        if (!mediaPlayer.isPlaying()) {
            //Request audio focus
            if (requestAudioFocus()) {
                //Could not gain focus
                stopSelf();
            }
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    private void pauseOrResumeAudio() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying())
                pauseMedia();
            else
                resumeMedia();
        }
    }

    private void seekAudio(int seekTo) {
        if (mediaPlayer != null) {
            final int finalSeekTo = Math.round((((float) seekTo) / 100) * mediaPlayer.getDuration());
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.seekTo(finalSeekTo);
            } else {
                resumePosition = finalSeekTo;
            }
        }
    }

    private void playAudio(Intent intent) {
        if (requestAudioFocus()) stopSelf();

        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.reset();
        }

        try {
            // We get the episode and the podcast objects passed through Intent
            episode = intent.getExtras().getParcelable("episode");
            podcast = intent.getExtras().getParcelable("podcast");
            // The episode file we are going to play
            mediaFile = episode.file;
        } catch (NullPointerException e) {
            stopSelf();
        }

        if (mediaFile != null && !mediaFile.equals("")) {
            initMediaPlayer();
            mediaPlayer.setOnPreparedListener(mp -> {
                // Seek to the saved position if it's valid
                if (episode.position > 0 && episode.position < mediaPlayer.getDuration())
                    mediaPlayer.seekTo(episode.position);

                playMedia();
            });
        }

    }

    private void fastForwardAudio() {
        if (mediaPlayer.isPlaying())
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() + fastForwardSeconds*1000);
        else
            resumePosition += fastForwardSeconds*1000;
    }

    private void rewindAudio() {
        if (mediaPlayer.isPlaying())
            mediaPlayer.seekTo(mediaPlayer.getCurrentPosition() - fastForwardSeconds*1000);
        else {
            if (resumePosition - fastForwardSeconds * 1000 < 0)
                resumePosition = 0;
            else
                resumePosition -= fastForwardSeconds * 1000;
        }
    }

    //The system calls this method when an activity requests the service be started
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        if (mediaSessionManager == null) {
            try {
                initMediaSession();
            } catch (RemoteException e) {
                e.printStackTrace();
                stopSelf();
            }
        }
        playAudio(intent);

        //Handle Intent action from MediaSession.TransportControls
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("MEDIAAUDIO", "STOPPED");
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        //Disable the PhoneStateListener
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }

        removeNotification();
        //unregister BroadcastReceivers
        unregisterReceivers();
        handlerRefreshUI.removeCallbacksAndMessages(null);
    }


// DATABASE OPERATIONS

    private void saveEpisodePosition() {
        if (mediaPlayer == null || episode == null || podcast == null) return;
        int pos;

        if (mediaPlayer.isPlaying())
            pos = mediaPlayer.getCurrentPosition();
        else
            pos = resumePosition;

        try {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            dbh.updateEpisodePosition(episode.id, pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void resetEpisodePosition() {
        if (mediaPlayer == null || episode == null || podcast == null) return;
        try {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            dbh.updateEpisodePosition(episode.id, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


// NOTIFICATIONS

    private void initMediaSession() {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        mediaSession.setActive(true);
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        //Set mediaSession's MetaData
//        updateMetaData();

        // Attach Callback to receive MediaSession updates
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            // Implement callbacks
            @Override
            public void onPlay() {
                super.onPlay();
                pauseOrResumeAudio();
            }

            @Override
            public void onPause() {
                super.onPause();
                pauseOrResumeAudio();
            }

            @Override
            public void onFastForward() {
                super.onFastForward();
                fastForwardAudio();
            }

            @Override
            public void onRewind() {
                super.onRewind();
                rewindAudio();
            }

            @Override
            public void onStop() {
                super.onStop();
                removeNotification();
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });
    }

    private void updateMetaData() {
        //replace with medias albumArt
//        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.drawable.image);
        // Update the current metadata
//        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
//                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
//                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
//                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
//                .build());
    }

    private void buildNotification(int playbackStatus) {
        int iconPlayPause = R.drawable.ic_pause_black_24dp;
        PendingIntent play_pauseAction = null;

        //Build a new notification according to the current state of the MediaPlayer
        if (playbackStatus == PAUSED) {
            iconPlayPause = R.drawable.ic_play_arrow_black_24dp;
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.ic_poddy); //replace with your own image

        String channelId = "com.guille.poddy.notification";

        // Create a new Notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, channelId)
                .setShowWhen(false)
                // Set the Notification style
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        // Attach our MediaSession token
                        .setMediaSession(mediaSession.getSessionToken())
                        // Show up to 3 buttons in the compacted version of the notification
                        .setShowActionsInCompactView(0, 1, 2))
                // Set the Notification color
                .setColor(getResources().getColor(R.color.colorPrimary))
                // Set the large and small icons
                .setLargeIcon(largeIcon)
                .setSmallIcon(R.drawable.ic_poddy)
                // Set Notification content information
                .setContentTitle(podcast.title)
                .setContentText(episode.title)
//                .setContentInfo("info")
                // Add playback actions
                .addAction(R.drawable.ic_fast_rewind_black_24dp, "back", playbackAction(1))
                .addAction(iconPlayPause, "pause", playbackAction(0))
                .addAction(R.drawable.ic_fast_forward_black_24dp, "forward", playbackAction(2))
                .setPriority(NotificationCompat.PRIORITY_MAX);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "name";
            String description = "description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager.createNotificationChannel(channel);
        }
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    private void removeNotification() {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private PendingIntent playbackAction(int actionNumber) {
        Intent playbackAction = new Intent(this, MediaPlayerService.class);
        switch (actionNumber) {
            case 0:
                // Pause/Resume
                playbackAction.setAction(Broadcast.PAUSE_OR_RESUME);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 1:
                // Fast rewind
                playbackAction.setAction(Broadcast.REWIND);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Fast forwards
                playbackAction.setAction(Broadcast.FAST_FORWARD);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

    private void handleIncomingActions(Intent playbackAction) {
        if (playbackAction == null || playbackAction.getAction() == null) return;
        String actionString = playbackAction.getAction();

        if (actionString.equalsIgnoreCase(Broadcast.PAUSE_OR_RESUME)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(Broadcast.REWIND)) {
            transportControls.rewind();
        } else if (actionString.equalsIgnoreCase(Broadcast.FAST_FORWARD)) {
            transportControls.fastForward();
        }
    }

    // Overridden unimportant methods

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed.
        stopMedia(false);
        //stop the service
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //Invoked when there has been an error during an asynchronous operation
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        //Invoked when the media source is ready for playback.
        playMedia();
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        //Invoked to communicate some info.
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        //Invoked indicating the completion of a seek operation.
    }

    @Override
    public void onAudioFocusChange(int focusState) {
        //Invoked when the audio focus of the system is updated.
        switch (focusState) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) resumeMedia();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
//                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
//                mediaPlayer.release();
//                mediaPlayer = null;
                if (mediaPlayer.isPlaying()) pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) pauseMedia();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private boolean requestAudioFocus() {
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        //Focus gained
        return result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        //Could not gain focus
    }

    private boolean removeAudioFocus() {
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                audioManager.abandonAudioFocus(this);
    }


    // BROADCAST RECEIVERS

    private final BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            playAudio(intent);
        }
    };

    private final BroadcastReceiver pauseOrResumeAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transportControls.pause();
        }
    };

    private final BroadcastReceiver rewindAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transportControls.rewind();
        }
    };

    private final BroadcastReceiver fastForwardAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            transportControls.fastForward();
        }
    };

    private final BroadcastReceiver seekAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            seekAudio(intent.getExtras().getInt("seekTo"));
        }
    };

    private BroadcastReceiver requestRefreshUIReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshUI();
        }
    };

    private void registerReceivers() {
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.registerReceiver(playNewAudio, new IntentFilter(Broadcast.PLAY_NEW_AUDIO));
        bm.registerReceiver(pauseOrResumeAudio, new IntentFilter(Broadcast.PAUSE_OR_RESUME));
        bm.registerReceiver(fastForwardAudio, new IntentFilter(Broadcast.FAST_FORWARD));
        bm.registerReceiver(rewindAudio, new IntentFilter(Broadcast.REWIND));
        bm.registerReceiver(seekAudio, new IntentFilter(Broadcast.SEEK));
        // NOT LOCAL
        registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    private void unregisterReceivers() {
        final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
        bm.unregisterReceiver(playNewAudio);
        bm.unregisterReceiver(pauseOrResumeAudio);
        bm.unregisterReceiver(fastForwardAudio);
        bm.unregisterReceiver(rewindAudio);
        bm.unregisterReceiver(seekAudio);
        // NOT LOCAL
        unregisterReceiver(becomingNoisyReceiver);
    }

    private void refreshUI() {
        if (mediaPlayer != null) {
            // Send broadcast
            Intent intent = new Intent(Broadcast.REFRESH_MEDIAPLAYER);
            if (mediaPlayer.isPlaying()) {
                intent.putExtra("currentPosition", mediaPlayer.getCurrentPosition());
                intent.putExtra("status", PLAYING);
            } else {
                intent.putExtra("currentPosition", resumePosition);
                intent.putExtra("status", PAUSED);
            }

            intent.putExtra("duration", mediaPlayer.getDuration());
            intent.putExtra("episode", episode);
            intent.putExtra("podcast", podcast);
            final LocalBroadcastManager bm = LocalBroadcastManager.getInstance(getApplicationContext());
            bm.sendBroadcast(intent);
        }
    }

    // As long as the service is active,
    // this handler keeps sending info of what's being played to the FragmentMediaPlayerBar
    private void registerUIRefresher() {
        handlerRefreshUI.postDelayed(new Runnable() {
            @Override
            public void run() {
                refreshUI();
                Log.i("REFRESHING", "REFRESHING MEDIA UI");
                handlerRefreshUI.postDelayed(this, UI_REFRESH_DELAY);
            }
        }, UI_REFRESH_DELAY);
    }

    // Broadcast receivers

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //pause audio on ACTION_AUDIO_BECOMING_NOISY
            pauseMedia();
        }
    };

    // Incoming phone calls
    private void callStateListener() {
        // Get the telephony manager
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        //Starting listening for PhoneState changes
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    //if at least one call exists or the phone is ringing
                    //pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Phone idle. Start playing.
                        if (mediaPlayer != null) {
                            if (ongoingCall) {
                                ongoingCall = false;
                                resumeMedia();
                            }
                        }
                        break;
                }
            }
        };
        // Register the listener with the telephony manager
        // Listen for changes to the device call state.
        telephonyManager.listen(phoneStateListener,
                PhoneStateListener.LISTEN_CALL_STATE);
    }


    // Binder given to clients
    private final IBinder iBinder = new LocalBinder();
    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }
    public class LocalBinder extends Binder {
        public MediaPlayerService getService() {
            return MediaPlayerService.this;
        }
    }
}