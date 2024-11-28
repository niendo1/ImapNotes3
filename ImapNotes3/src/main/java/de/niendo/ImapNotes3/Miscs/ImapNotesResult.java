/*
 * Copyright (C) 2022-2024 - Peter Korf <peter@niendo.de>
 * Copyright (C)      2016 - Axel Strübing
 * Copyright (C)      2016 - Martin Carpella
 * Copyright (C)      2015 - nb
 * and Contributors.
 *
 * This file is part of ImapNotes3.
 *
 * ImapNotes3 is free software: you can redistribute it and/or modify
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.niendo.ImapNotes3.Miscs;

import androidx.annotation.NonNull;

public class ImapNotesResult {

    public static final int ResultCodeSuccess = 0;
    public static final int ResultCodeException = -2;
    public static final int ResultCodeCantConnect = -1;
    public static final int ResultCodeImapFolderCreated = 1;

    public final int returnCode;
    @NonNull
    public final String errorMessage;
    public final Long UIDValidity;

    public ImapNotesResult(int returnCode,
                           String errorMessage,
                           long UIDValidity) {
        this.returnCode = returnCode;
        this.errorMessage = errorMessage;
        this.UIDValidity = UIDValidity;
    }

}
