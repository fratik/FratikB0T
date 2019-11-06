/*
 * Copyright (C) 2019 FratikB0T Contributors
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

package pl.fratik.api.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import gg.amy.pgorm.annotations.GIndex;
import gg.amy.pgorm.annotations.PrimaryKey;
import gg.amy.pgorm.annotations.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import pl.fratik.core.entity.DatabaseEntity;

import java.beans.Transient;
import java.util.ArrayList;
import java.util.List;

@Data
@Table("rundki")
@GIndex("idRundki")
@AllArgsConstructor
public class Rundka implements DatabaseEntity {
    @PrimaryKey("idRundki")
    private final Integer idRundki;
    private boolean trwa;
    private List<RundkaOdpowiedzFull> zgloszenia = new ArrayList<>();
    private String voteChannel;
    private String normalChannel;

    public Rundka(Integer idRundki, boolean trwa) {
        this.idRundki = idRundki;
        this.trwa = trwa;
    }

    @Override
    @JsonIgnore
    @Transient
    public String getTableName() {
        return "rundki";
    }

    @Data
    @RequiredArgsConstructor
    @AllArgsConstructor
    public static class RundkaWrapper {
        private final Integer idRundki;
        private final boolean wToku;
        private List<RundkaOdpowiedz> zgloszenia = new ArrayList<>();
        public RundkaWrapper(Rundka r) {
            idRundki = r.idRundki;
            wToku = r.trwa;
            zgloszenia.addAll(r.zgloszenia);
        }
    }
}
