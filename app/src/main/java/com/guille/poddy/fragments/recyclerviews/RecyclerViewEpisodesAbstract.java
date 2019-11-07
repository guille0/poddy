package com.guille.poddy.fragments.recyclerviews;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.lang.Object;

import androidx.recyclerview.widget.RecyclerView;

import com.guille.poddy.Helpers;
import com.guille.poddy.services.*;
import com.guille.poddy.*;
import com.guille.poddy.R;
import com.guille.poddy.database.Episode;
import com.tonyodev.fetch2.Download;

import java.util.ArrayList;
import java.util.List;

public abstract class RecyclerViewEpisodesAbstract extends RecyclerView.Adapter<RecyclerViewEpisodesAbstract.ViewHolder>{

    public static final int CLICK_IMAGE = 0;
    public static final int CLICK_TEXT = 1;

    protected List<Episode> episodes;
    protected LayoutInflater inflater;
    protected ItemClickListener clickListener;

    protected long listeningEpisodeId = -1;
    protected int listeningStatus = -1;

    protected List<Download> downloading = new ArrayList<>();

    protected void setEpisode(int position, Episode ep) {
        episodes.set(position, ep);
    }

    protected void setDownloadingList(List<Download> list) {
        downloading = list;
    }

    public List<Episode> getItems() {
        return episodes;
    }

    public void setItems(List<Episode> eps) {
        episodes = eps;
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    // allows clicks events to be caught
    protected void setClickListener(ItemClickListener itemClickListener) {
        clickListener = itemClickListener;
    }

    public void setListeningStatus(long episodeId, int status) {
        listeningStatus = status;
        listeningEpisodeId = episodeId;
    }

    // binds the data to the TextView in each row
    protected void updateProgress(ViewHolder holder, Episode ep) {
        final Download download = Helpers.isBeingDownloaded(this.downloading, ep.id);
        if (download == null) {
            holder.progressBar.setProgress(0);
            holder.progressBar.setVisibility(View.GONE);
        } else {
            holder.progressBar.setProgress(download.getProgress());
            holder.progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position, List<Object> payloads) {
        Episode ep = episodes.get(position);
        updateProgress(holder, ep);

        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
            return;
        }

        for (Object payload : payloads) {
            if (payload instanceof String) {
                switch ((String) payload) {
                    case FragmentEpisodes.UPDATE_ALL:
                        onBindViewHolder(holder, position);
                        break;
                    case FragmentEpisodes.UPDATE_STATUS:
                        updateStatus(holder, ep);
                        break;
                }
            }
        }
    }

    protected void updateStatus(ViewHolder holder, Episode ep) {
        final int STARTPLAY = 1;
        final int RESUME = 2;
        final int PAUSE = 3;
        final int DOWNLOAD = 4;
        final int CANCEL = 5;
        int activeButton;

        if (holder.textDuration != null) {
            holder.textDuration.setText(Helpers.milisecondsToString(ep.duration));
        }

        if (ep.downloaded) {
            if (listeningEpisodeId == ep.id && listeningStatus != MediaPlayerService.STOPPED) {
                if (listeningStatus == MediaPlayerService.PAUSED)
                    activeButton = RESUME;
                else
                    activeButton = PAUSE;
            } else
                activeButton = STARTPLAY;
            holder.progressBar.setVisibility(View.GONE);
        } else {
            final Download download = Helpers.isBeingDownloaded(this.downloading, ep.id);
            if (download != null) {
                activeButton = CANCEL;
                holder.progressBar.setVisibility(View.VISIBLE);
            } else {
                activeButton = DOWNLOAD;
                holder.progressBar.setVisibility(View.GONE);
            }
        }
        switch (activeButton) {
            case STARTPLAY:
                holder.buttonResume.setVisibility(View.GONE);
                holder.buttonPause.setVisibility(View.GONE);
                holder.buttonDownload.setVisibility(View.GONE);
                holder.buttonCancel.setVisibility(View.GONE);
                holder.buttonStartPlay.setVisibility(View.VISIBLE);
                break;
            case RESUME:
                holder.buttonStartPlay.setVisibility(View.GONE);
                holder.buttonPause.setVisibility(View.GONE);
                holder.buttonDownload.setVisibility(View.GONE);
                holder.buttonCancel.setVisibility(View.GONE);
                holder.buttonResume.setVisibility(View.VISIBLE);
                break;
            case PAUSE:
                holder.buttonStartPlay.setVisibility(View.GONE);
                holder.buttonDownload.setVisibility(View.GONE);
                holder.buttonCancel.setVisibility(View.GONE);
                holder.buttonResume.setVisibility(View.GONE);
                holder.buttonPause.setVisibility(View.VISIBLE);
                break;
            case DOWNLOAD:
                holder.buttonStartPlay.setVisibility(View.GONE);
                holder.buttonCancel.setVisibility(View.GONE);
                holder.buttonResume.setVisibility(View.GONE);
                holder.buttonPause.setVisibility(View.GONE);
                holder.buttonDownload.setVisibility(View.VISIBLE);
                break;
            case CANCEL:
                holder.buttonStartPlay.setVisibility(View.GONE);
                holder.buttonResume.setVisibility(View.GONE);
                holder.buttonPause.setVisibility(View.GONE);
                holder.buttonDownload.setVisibility(View.GONE);
                holder.buttonCancel.setVisibility(View.VISIBLE);
                break;
        }
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
        void onDownloadClick(View view, int position);
        void onCancelClick(View view, int position);
        void onStartPlayClick(View view, int position);
        void onPlayClick(View view, int position);
        void onPauseClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    protected class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        protected TextView textEpisodeTitle, textPodcastTitle, textDate, textDuration;
        protected ImageButton buttonResume, buttonPause, buttonDownload, buttonCancel, buttonStartPlay;
        protected ProgressBar progressBar;
        protected ViewGroup viewGroup;

        public ViewHolder(View itemView) {
            super(itemView);
            textEpisodeTitle = itemView.findViewById(R.id.textEpisodeTitle);
            textDuration = itemView.findViewById(R.id.textDuration);
            buttonResume = itemView.findViewById(R.id.buttonResume);
            buttonPause = itemView.findViewById(R.id.buttonPause);
            buttonDownload = itemView.findViewById(R.id.buttonDownload);
            buttonCancel = itemView.findViewById(R.id.buttonCancel);
            buttonStartPlay = itemView.findViewById(R.id.buttonStartPlay);

            progressBar = itemView.findViewById(R.id.progressBar);
            viewGroup = itemView.findViewById(R.id.layoutText);

            viewGroup.setOnClickListener(this);
            buttonStartPlay.setOnClickListener(this);
            buttonCancel.setOnClickListener(this);
            buttonDownload.setOnClickListener(this);
            buttonResume.setOnClickListener(this);
            buttonPause.setOnClickListener(this);

            textPodcastTitle = itemView.findViewById(R.id.textPodcastTitle);
            textDate = itemView.findViewById(R.id.textDate);
        }

        @Override
        public void onClick(View view) {
            if (clickListener != null) {
                if (view.getId() == buttonStartPlay.getId()) {
                    clickListener.onStartPlayClick(view, getAdapterPosition());
                } else if (view.getId() == buttonDownload.getId()) {
                    clickListener.onDownloadClick(view, getAdapterPosition());
                } else if (view.getId() == buttonCancel.getId()) {
                    clickListener.onCancelClick(view, getAdapterPosition());
                } else if (view.getId() == buttonResume.getId()) {
                    clickListener.onPlayClick(view, getAdapterPosition());
                } else if (view.getId() == buttonPause.getId()) {
                    clickListener.onPauseClick(view, getAdapterPosition());
                } else {
                    clickListener.onItemClick(view, getAdapterPosition());
                }
            }
        }
    }

}
