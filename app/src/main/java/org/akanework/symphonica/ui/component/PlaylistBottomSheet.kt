/*
 *     Copyright (C) 2023 AkaneWork Organization
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.akanework.symphonica.ui.component

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.akanework.symphonica.MainActivity.Companion.playlistViewModel
import org.akanework.symphonica.R
import org.akanework.symphonica.SymphonicaApplication
import org.akanework.symphonica.ui.adapter.PlaylistAdapter


class PlaylistBottomSheet : BottomSheetDialogFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val rootView = inflater.inflate(R.layout.playlist_bottom_sheet, container, false)
        playlistView = rootView.findViewById(R.id.playlist_recyclerview)
        playlistView.layoutManager = LinearLayoutManager(SymphonicaApplication.context)
        playlistView.adapter = PlaylistAdapter(playlistViewModel.playList)
        scrollToCurrent()
        return rootView
    }

    companion object {
        const val TAG = "PlaylistBottomSheet"
        lateinit var playlistView: RecyclerView
        fun scrollToCurrent() {
            playlistView.layoutManager!!.scrollToPosition(playlistViewModel.currentLocation)
        }
    }
}