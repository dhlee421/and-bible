/*
 * Copyright (c) 2020 Martin Denham, Tuomas Airaksinen and the And Bible contributors.
 *
 * This file is part of And Bible (http://github.com/AndBible/and-bible).
 *
 * And Bible is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 *
 * And Bible is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with And Bible.
 * If not, see http://www.gnu.org/licenses/.
 *
 */
package net.bible.android.view.activity.download

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.document_list_item.view.*
import net.bible.android.activity.R
import net.bible.android.control.download.DocumentStatus
import net.bible.android.control.download.DocumentStatus.DocumentInstallStatus
import net.bible.android.control.event.ABEventBus
import net.bible.android.control.event.documentdownload.DocumentDownloadEvent
import net.bible.android.view.activity.base.RecommendedDocuments
import org.crosswire.jsword.book.Book
import org.crosswire.jsword.book.BookCategory

val Book.imageResource: Int
    get() = when(bookCategory) {
            BookCategory.BIBLE -> R.drawable.ic_bible
            BookCategory.COMMENTARY -> R.drawable.ic_commentary
            BookCategory.DICTIONARY -> R.drawable.ic_dictionary
            BookCategory.MAPS -> R.drawable.ic_map_black_24dp
            BookCategory.GENERAL_BOOK -> R.drawable.ic_book
            else -> R.drawable.ic_book
        }

fun Book.isRecommended(recommendedDocuments: RecommendedDocuments): Boolean
{
    val osisIdKey = osisID.split(".")[1]
    return recommendedDocuments
        .getForBookCategory(bookCategory)[language.code]?.contains(osisIdKey) == true
}

/** Add an image to the normal 2 line list item
 *
 * @author Martin Denham [mjdenham at gmail dot com]
 */
class DocumentListItem(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {
    lateinit var recommendedDocuments: RecommendedDocuments

    lateinit var document: Book

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ensureRegisteredForDownloadEvents()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ABEventBus.getDefault().unregister(this)
    }

    fun onEventMainThread(event: DocumentDownloadEvent) {
        if (event.initials == document.initials) {
            updateControlState(event.documentStatus)
        }
    }

    fun setIcons() {
        val docImage = document.imageResource
        documentTypeIcon.setImageResource(docImage)

        val isRecommended = document.isRecommended(recommendedDocuments)
        recommendedIcon.visibility = if(isRecommended) View.VISIBLE else View.INVISIBLE
        documentLanguage.text = document.language.name
    }

    fun updateControlState(documentStatus: DocumentStatus) {
        when (documentStatus.documentInstallStatus) {
            DocumentInstallStatus.INSTALLED -> {
                downloadStatusIcon.setImageResource(R.drawable.ic_check_green_24dp)
                progressBar.visibility = View.INVISIBLE
            }
            DocumentInstallStatus.NOT_INSTALLED -> {
                downloadStatusIcon.setImageDrawable(null)
                progressBar.visibility = View.INVISIBLE
            }
            DocumentInstallStatus.BEING_INSTALLED -> {
                downloadStatusIcon.setImageResource(R.drawable.ic_arrow_downward_green_24dp)
                setProgressPercent(documentStatus.percentDone)
                progressBar.visibility = View.VISIBLE
            }
            DocumentInstallStatus.UPGRADE_AVAILABLE -> {
                downloadStatusIcon.setImageResource(R.drawable.ic_arrow_upward_amber_24dp)
                progressBar.visibility = View.INVISIBLE
            }
            DocumentInstallStatus.ERROR_DOWNLOADING -> {
                downloadStatusIcon.setImageResource(R.drawable.ic_warning_red_24dp)
                progressBar.visibility = View.INVISIBLE
            }
        }
    }

    /**
     * Should not need to check the initials but other items were being updated and I don't know why
     */
    private fun setProgressPercent(percentDone: Int) {
        progressBar.progress = percentDone
    }

    /**
     * Items are detached more often than inflated so always have to check the item is registered for download events.
     * https://code.google.com/p/android/issues/detail?id=65617
     */
    private fun ensureRegisteredForDownloadEvents() {
        ABEventBus.getDefault().safelyRegister(this)
    }
}
