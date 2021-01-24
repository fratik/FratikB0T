/*
 * Copyright (C) 2019-2021 FratikB0T Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package pl.fratik.music.entity;

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import pl.fratik.core.tlumaczenia.Language;
import pl.fratik.music.managers.SearchManager;

import java.util.Objects;

@Getter
@AllArgsConstructor
public class PiosenkaImpl implements Piosenka {
    @NotNull private final String requester;
    @NotNull private final AudioTrack audioTrack;
    @NotNull private final Language requesterLanguage;
    private final boolean youtube;
    private String thumbnailURL = null;

    public PiosenkaImpl(@NotNull String requester, @NotNull AudioTrack track, @NotNull Language language) {
        this.requester = requester;
        this.audioTrack = track;
        this.requesterLanguage = language;
        youtube = track instanceof YoutubeAudioTrack;
    }

    public PiosenkaImpl(@NotNull String requester, @NotNull AudioTrack track, @NotNull Language language, String thumbnailUrl) {
        this.requester = requester;
        this.audioTrack = track;
        this.requesterLanguage = language;
        this.thumbnailURL = thumbnailUrl;
        youtube = track instanceof YoutubeAudioTrack;
    }

    @Override
    public void fillThumbnailURL(SearchManager searchManager) {
        if (thumbnailURL != null || !youtube) return;
        thumbnailURL = searchManager.getThumbnail(searchManager.extractIdFromUri(audioTrack.getInfo().uri)).getThumbnailURL();
    }

    @SuppressWarnings("all")
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof PiosenkaImpl)) return false;
        final PiosenkaImpl other = (PiosenkaImpl) o;
        if (!other.canEqual(this)) return false;
        final Object this$requester = this.getRequester();
        final Object other$requester = other.getRequester();
        if (!Objects.equals(this$requester, other$requester)) return false;
        final Object this$audioTrack = this.getAudioTrack();
        final Object other$audioTrack = other.getAudioTrack();
        if (!Objects.equals(this$audioTrack, other$audioTrack))
            return false;
        final Object this$requesterLanguage = this.getRequesterLanguage();
        final Object other$requesterLanguage = other.getRequesterLanguage();
        if (!Objects.equals(this$requesterLanguage, other$requesterLanguage))
            return false;
        final Object this$youtube = this.isYoutube();
        final Object other$youtube = other.isYoutube();
        if (!Objects.equals(this$youtube, other$youtube))
            return false;
        final Object this$thumbnailURL = this.getThumbnailURL();
        final Object other$thumbnailURL = other.getThumbnailURL();
        return Objects.equals(this$thumbnailURL, other$thumbnailURL);
    }

    @SuppressWarnings("all")
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $requester = this.getRequester();
        result = result * PRIME + ($requester == null ? 43 : $requester.hashCode());
        final Object $audioTrack = this.getAudioTrack();
        result = result * PRIME + ($audioTrack == null ? 43 : $audioTrack.hashCode());
        final Object $requesterLanguage = this.getRequesterLanguage();
        result = result * PRIME + ($requesterLanguage == null ? 43 : $requesterLanguage.hashCode());
        final Object $youtube = this.isYoutube();
        result = result * PRIME + ($youtube == null ? 43 : $youtube.hashCode());
        final Object $thumbnailURL = this.getThumbnailURL();
        result = result * PRIME + ($thumbnailURL == null ? 43 : $thumbnailURL.hashCode());
        return result;
    }

    @SuppressWarnings("all")
    private boolean canEqual(Object other) {
        return other instanceof PiosenkaImpl;
    }

    public String toString() {
        return "Piosenka(requester=" + this.getRequester() + ", audioTrack=" + this.getAudioTrack() +
                ", requesterLanguage=" + this.getRequesterLanguage() + ", youtube=" + this.youtube +
                ", thumbnailURL=" + this.getThumbnailURL() + ")";
    }
}

