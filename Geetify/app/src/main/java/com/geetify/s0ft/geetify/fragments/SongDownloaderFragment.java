package com.geetify.s0ft.geetify.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.geetify.s0ft.geetify.network.HttpGetMp3;
import com.geetify.s0ft.geetify.LibrarySongsManager;
import com.geetify.s0ft.geetify.MainActivity;
import com.geetify.s0ft.geetify.R;
import com.geetify.s0ft.geetify.datamodels.YoutubeSong;
import com.geetify.s0ft.geetify.exceptions.CannotCreateFolderOnExternalStorageException;
import com.geetify.s0ft.geetify.exceptions.ExternalStorageNotFoundException;
import com.geetify.s0ft.geetify.helpers.AppSettings;
import com.geetify.s0ft.geetify.helpers.HelperClass;

/**
 * Created by s0ft on 2/22/2018.
 */

public class SongDownloaderFragment extends Fragment implements HttpGetMp3.MP3DownloadListener {

    public interface OnPlayDownloadedSongListener {
        void onDownloadedMP3PlayRequested();
    }

    private OnPlayDownloadedSongListener onPlayDownloadedSongListener;

    private YoutubeSong currentSong = null;
    private boolean songDownloadedFlag = false;
    private boolean saveSongToLibraryFlag = false;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            this.onPlayDownloadedSongListener = (OnPlayDownloadedSongListener) activity;
            songDownloadedFlag = false;
            saveSongToLibraryFlag = false;
        } catch (ClassCastException ccex) {
            throw new ClassCastException(activity.toString() + " must implement OnPlayDownloadedSongListener!");
        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_songdownloader, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.findItem(R.id.searchIcon).setVisible(false);
        inflater.inflate(R.menu.option_menu_song_downloader_fragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.addSongToLibrary) {
            try {
                if (songDownloadedFlag) {
                    if (new LibrarySongsManager(getActivity()).addLibrarySong(currentSong))
                        Toast.makeText(getActivity(), "Added to library.", Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getActivity(), "Oops! Something went wrong.", Toast.LENGTH_SHORT).show();
                } else {
                    saveSongToLibraryFlag = true;
                    Toast.makeText(getActivity(), "Will do, when the song is finished downloading.", Toast.LENGTH_SHORT).show();
                }
            } catch (CannotCreateFolderOnExternalStorageException ccfoesex) {
                Toast.makeText(getActivity(), "Cannot create folder on external storage.", Toast.LENGTH_SHORT).show();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.currentSong = ((MainActivity) getActivity()).getcurrentSong();

        ((ImageView) getView().findViewById(R.id.songthumbnail)).setImageBitmap(this.currentSong.getHqThumbnailBitmap());
        ((TextView) getView().findViewById(R.id.songtitle)).setText(this.currentSong.getTitle());
        ((TextView) getView().findViewById(R.id.songdescription)).setText(this.currentSong.getDescription());


        downloadAndPlayMP3(this.currentSong);

    }

    private void downloadAndPlayMP3(YoutubeSong currentSong) {
        try {
            new HttpGetMp3(this, getActivity()).execute(currentSong.getVideoId(), AppSettings.getMP3StoragePath() + HelperClass.getValidFilename(currentSong.getTitle()) + ".webm");
        } catch (CannotCreateFolderOnExternalStorageException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onProgressUpdate(String progressMessage) {
        String progressReporterString = progressMessage;

        Log.w("YTS", progressReporterString);
        ((TextView) getView().findViewById(R.id.songdescription)).setText(progressReporterString);
    }

    @Override
    public void onDownloadError(String error) {
        Log.w("YTS", error);
        HelperClass.WriteToLog(error);
        Toast.makeText(getActivity(), "Song could not be downloaded for some reason. Log file updated.", Toast.LENGTH_LONG).show();
        ((TextView) getView().findViewById(R.id.songdescription)).setText(currentSong.getDescription());
    }

    @Override
    public void onDownloadFinished() {
        songDownloadedFlag = true;
        if (saveSongToLibraryFlag || PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_savedownloads", false)) {
            try {
                if (new LibrarySongsManager(getActivity()).addLibrarySong(currentSong))
                    Toast.makeText(getActivity(), "Added to library.", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getActivity(), "Something went wrong while trying to save to library.", Toast.LENGTH_SHORT).show();
            } catch (CannotCreateFolderOnExternalStorageException ccfoesex) {
                Toast.makeText(getActivity(), "Cannot create folder on external storage.", Toast.LENGTH_SHORT).show();
            }

        }

        ((TextView) getView().findViewById(R.id.songdescription)).setText(currentSong.getDescription());

        if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("pref_autoplayafterdownload", true))
            playCurrentSong();
    }

    private void playCurrentSong() {
        this.onPlayDownloadedSongListener.onDownloadedMP3PlayRequested();
    }
}
