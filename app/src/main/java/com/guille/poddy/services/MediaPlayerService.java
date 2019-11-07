package com.guille.poddy.services;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.*;
import android.content.*;
import android.app.*;
import androidx.core.content.ContextCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import androidx.media.app.NotificationCompat.MediaStyle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.pm.ServiceInfo;
import androidx.annotation.RequiresApi;

import androidx.media.session.*;
import android.media.session.PlaybackState;

import android.support.v4.media.session.PlaybackStateCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.Action;

import com.guille.poddy.R;
import com.guille.poddy.database.*;
import com.guille.poddy.activities.*;

import com.guille.poddy.eventbus.*;
import org.greenrobot.eventbus.*;

import java.io.IOException;

public class MediaPlayerService extends Service implements MediaPlayer.OnCompletionListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener, MediaPlayer.OnSeekCompleteListener,
        MediaPlayer.OnInfoListener, MediaPlayer.OnBufferingUpdateListener,
        AudioManager.OnAudioFocusChangeListener {

    // Notifications
    public static final String CHANNEL_ID = "poddy.ForegroundMediaPlayerService";
    private static final int NOTIFICATION_ID = 1;
    private NotificationCompat.Builder notificationBuilder;

    // Notification buttons
    private Action actionFastForward, actionRewind, actionPause, actionResume, actionStop;

    // Status
    public static final int PAUSED = 0;
    public static final int PLAYING = 1;
    public static final int STOPPED = 2;

    // Controls
    public static final String BROADCAST_NOTIFICATION_BUTTON = "com.guille.poddy.MediaPlayerService.NotificationButton";
    public static final String PAUSE_OR_RESUME = "PAUSE_OR_RESUME";
    public static final String REWIND = "REWIND";
    public static final String FAST_FORWARD = "FAST_FORWARD";
    public static final String STOP = "STOP";

    private static final int UI_REFRESH_DELAY = 500;
    private final Handler handlerRefreshUI = new Handler();

    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;

    // MediaSession
    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat mediaSession;
    private MediaControllerCompat.TransportControls transportControls;

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

    private final BroadcastReceiver notificationControls = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getExtras().getString("action")) {
                case PAUSE_OR_RESUME:
                    transportControls.pause();
                    break;
                case REWIND:
                    transportControls.rewind();
                    break;
                case FAST_FORWARD:
                    transportControls.fastForward();
                    break;
                case STOP:
                    transportControls.stop();
                    break;
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // Detects telephone calls
        registerTelephonyListener();
        // Detects headphones being plugged, unplugged, etc.
        registerReceiver(becomingNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        // Detects notification control buttons
        registerReceiver(notificationControls, new IntentFilter(BROADCAST_NOTIFICATION_BUTTON));
        // Detects eventbuses from activity
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();

        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        EventBus.getDefault().unregister(this);
        unregisterReceiver(becomingNoisyReceiver);
        unregisterReceiver(notificationControls);
        unregisterUIRefresher();
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.cancel(NOTIFICATION_ID);
        super.onDestroy();
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

        try {
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            stopSelf();
        }
    }

    // BASIC CONTROLS

    private void playMedia() {
//        buildNotification(PLAYING);
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            sendAudioBeingPlayedStatus();
            registerUIRefresher();
        }
    }

    private void stopMedia(Boolean savePosition) {
        if (mediaPlayer == null) return;

        if (savePosition)
            saveEpisodePosition();
        else
            resetEpisodePosition();

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        sendAudioBeingPlayedStatus(true);
        unregisterUIRefresher();
    }
    private void stopMedia() {
        stopMedia(true);
    }

    private void pauseMedia() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
            sendAudioBeingPlayedStatus();
            sendAudioBeingPlayedPosition();

            unregisterUIRefresher();
        }
    }

    private void resumeMedia() {
        if (!mediaPlayer.isPlaying()) {
            if (requestAudioFocus()) {
                stopSelf();
            }
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
            sendAudioBeingPlayedStatus();
            sendAudioBeingPlayedPosition();

            registerUIRefresher();
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

    private void seekAudio(long seekTo) {
        if (mediaPlayer != null) {
            final int finalSeekTo = Math.round((((float) seekTo) / 100) * mediaPlayer.getDuration());
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.seekTo(finalSeekTo);
            } else {
                resumePosition = finalSeekTo;
            }
            sendAudioBeingPlayedPosition();
        }
    }

    private void playNewAudio(long episodeId) {
        if (requestAudioFocus()) stopSelf();

        if (mediaPlayer != null) {
            stopMedia();
            mediaPlayer.reset();
        }

        final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());

        episode = dbh.getEpisodeFromId(episodeId);
        sendAudioBeingPlayedEpisode();
        // The episode file we are going to play
        mediaFile = episode.file;

        if (mediaFile != null && !mediaFile.equals("")) {
            initMediaPlayer();

            mediaPlayer.setOnPreparedListener(mp -> {
                // Seek to the saved position if it's valid
                if (episode.position > 0 && episode.position < mediaPlayer.getDuration())
                    mediaPlayer.seekTo((int) episode.position);

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
            } catch (Exception e) {
                e.printStackTrace();
                stopSelf();
            }
        }
        // Create a base notification just to start the service
        // It gets refreshed immediately, after loading the episode
        createNotificationChannel();
        // Creating action buttons
        createButtons();

        // On click get sent to ActivityMediaPlayer
        Intent notificationIntent = new Intent(this, ActivityMain.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Poddy")
                .setContentText("Loading media...")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_poddy)
                .setStyle(new MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(actionRewind)
                .addAction(actionPause)
                .addAction(actionFastForward)
                .addAction(actionStop)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MAX);

        startForeground(NOTIFICATION_ID, notificationBuilder.build());

        return START_STICKY;
    }

    private void createButtons() {
        actionPause = new Action.Builder(
                R.drawable.ic_pause_black_32dp,
                "Pause",
                PendingIntent.getBroadcast(this, 0,
                        new Intent(BROADCAST_NOTIFICATION_BUTTON).putExtra("action", PAUSE_OR_RESUME),
                        PendingIntent.FLAG_UPDATE_CURRENT)).build();

        actionResume = new Action.Builder(
                R.drawable.ic_play_arrow_black_32dp,
                "Resume",
                PendingIntent.getBroadcast(this, 3,
                        new Intent(BROADCAST_NOTIFICATION_BUTTON).putExtra("action", PAUSE_OR_RESUME),
                        PendingIntent.FLAG_UPDATE_CURRENT)).build();

        actionRewind = new Action.Builder(
                R.drawable.ic_fast_rewind_black_32dp,
                "Rewind",
                PendingIntent.getBroadcast(this, 1,
                        new Intent(BROADCAST_NOTIFICATION_BUTTON).putExtra("action", REWIND),
                        PendingIntent.FLAG_UPDATE_CURRENT)).build();

        actionFastForward = new Action.Builder(
                R.drawable.ic_fast_forward_black_32dp,
                "Fast forward",
                PendingIntent.getBroadcast(this, 2,
                        new Intent(BROADCAST_NOTIFICATION_BUTTON).putExtra("action", FAST_FORWARD),
                        PendingIntent.FLAG_UPDATE_CURRENT)).build();

        actionStop = new Action.Builder(
                R.drawable.ic_stop_black_32dp,
                "Stop",
                PendingIntent.getBroadcast(this, 4,
                        new Intent(BROADCAST_NOTIFICATION_BUTTON).putExtra("action", STOP),
                        PendingIntent.FLAG_UPDATE_CURRENT)).build();
    }

// DATABASE OPERATIONS

    private void saveEpisodePosition() {
        if (mediaPlayer == null || episode == null) return;
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
        if (mediaPlayer == null || episode == null) return;
        try {
            final DatabaseHelper dbh = DatabaseHelper.getInstance(getApplicationContext());
            dbh.updateEpisodePosition(episode.id, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

// NOTIFICATIONS

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    // EventBus senders (also update notification)

    private void sendAudioBeingPlayedEpisode() {
        EventBus.getDefault().postSticky(
                new MessageEvent.AudioBeingPlayedEpisode(episode));

        notificationBuilder
                .setContentTitle(episode.podcastTitle)
                .setContentText(episode.title);
        updateNotification();
    }

    private void sendAudioBeingPlayedStatus(boolean isStopped) {
        int status;

        if (mediaPlayer.isPlaying()) {
            status = PLAYING;
        } else {
            status = PAUSED;
        }
        if (isStopped) status = STOPPED;

        EventBus.getDefault().postSticky(
                new MessageEvent.AudioBeingPlayedStatus(episode.id, status));

        notificationBuilder.mActions.clear();
        notificationBuilder.addAction(actionRewind);

        if (status == PLAYING) notificationBuilder.addAction(actionPause);
        else notificationBuilder.addAction(actionResume);

        notificationBuilder
                .addAction(actionFastForward)
                .addAction(actionStop);

        updateNotification();
    }
    private void sendAudioBeingPlayedStatus() {
        sendAudioBeingPlayedStatus(false);
    }

    private void sendAudioBeingPlayedPosition() {
        if (mediaPlayer == null) return;
        long pos;
        final long duration = mediaPlayer.getDuration();

        if (mediaPlayer.isPlaying()) {
            pos = mediaPlayer.getCurrentPosition();
        } else {
            pos = resumePosition;
        }
        EventBus.getDefault().postSticky(
                new MessageEvent.AudioBeingPlayedPosition(episode.id, pos, duration));
    }

    private void initMediaSession() {
        if (mediaSessionManager != null) return; //mediaSessionManager exists

        mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        // Create a new MediaSession
        mediaSession = new MediaSessionCompat(getApplicationContext(), "AudioPlayer");
        //Get MediaSessions transport controls
        transportControls = mediaSession.getController().getTransportControls();
        //set MediaSession -> ready to receive media commands
        //indicate that the MediaSession handles transport control commands
        // through its MediaSessionCompat.Callback.
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

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
                sendAudioBeingPlayedStatus(true);
                //Stop the service
                stopSelf();
            }

            @Override
            public void onSeekTo(long position) {
                super.onSeekTo(position);
            }
        });


        mediaSession.setActive(true);
    }

//    private void updateMetaData() {
//        //replace with medias albumArt
////        Bitmap albumArt = BitmapFactory.decodeResource(getResources(), R.attr.ic_poddy);
//        // Update the current metadata
////        mediaSession.setMetadata(new MediaMetadataCompat.Builder()
////                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
////                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, activeAudio.getArtist())
////                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, activeAudio.getAlbum())
////                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, activeAudio.getTitle())
////                .build());
//    }

    // Overridden methods

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {
        //Invoked indicating buffering status of
        //a media resource being streamed over the network.
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        //Invoked when playback of a media source has completed.
        if (mediaPlayer.isPlaying()) {
            pauseMedia();
            seekAudio(0);
            resetEpisodePosition();
        } else {
            stopSelf();
        }
        sendAudioBeingPlayedStatus(false);
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
        if (audioManager == null) return false;
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceivePauseOrResume(MessageEvent.PauseOrResume event) {
        transportControls.pause();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveFastForward(MessageEvent.FastForward event) {
        transportControls.fastForward();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveRewind(MessageEvent.Rewind event) {
        transportControls.rewind();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveStop(MessageEvent.Stop event) {
        transportControls.stop();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceiveSeekTo(MessageEvent.SeekTo event) {
        seekAudio(event.position);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onReceivePlayNewAudio(MessageEvent.PlayNewAudio event) {
        playNewAudio(event.episodeId);
    }

    // As long as we are playing a file
    // this handler keeps sending the current position to the UI
    private void registerUIRefresher() {
        handlerRefreshUI.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendAudioBeingPlayedPosition();
                handlerRefreshUI.postDelayed(this, UI_REFRESH_DELAY);
            }
        }, UI_REFRESH_DELAY);
    }

    private void unregisterUIRefresher() {
        handlerRefreshUI.removeCallbacksAndMessages(null);
    }

    // Listeners...

    // Unplugging headphones, etc.
    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            pauseMedia();
        }
    };

    // Incoming phone calls
    private void registerTelephonyListener() {
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    // If we are in a call or the phone is ringing
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mediaPlayer != null) {
                            pauseMedia();
                            ongoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // When the call stops, resume
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
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }


    // Binder
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

    // Static methods

    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (MediaPlayerService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void coldPlayAudio(Context context, long episodeId) {
        if (!isServiceRunning(context)) {
            // Start the service and play the audio
            ServiceConnection serviceConnection = new ServiceConnection() {
                @Override
                public void onServiceDisconnected(ComponentName name) {}
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    MediaPlayerService.LocalBinder binder = (MediaPlayerService.LocalBinder) service;
                    binder.getService().playNewAudio(episodeId);
                    context.unbindService(this);
                }
            };

            Intent intent = new Intent(context, MediaPlayerService.class);

            ContextCompat.startForegroundService(context, intent);
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        } else {
            // Send a Bus with the audio
            EventBus.getDefault().post(new MessageEvent.PlayNewAudio(episodeId));
        }
    }

}