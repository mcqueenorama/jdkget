/*-
 * Copyright (C) 2008 Erik Larsson
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.takari.jdkget.osx.hfs.types.hfs;

import io.takari.jdkget.osx.hfs.types.hfsplus.HFSPlusDateField;

/**
 * @author <a href="http://www.catacombae.org/" target="_top">Erik Larsson</a>
 */
public class HFSDateField extends HFSPlusDateField {
  public HFSDateField(byte[] data) {
    this(data, 0, data.length);
  }

  public HFSDateField(byte[] data, int offset, int length) {
    super("HFSDate", data, offset, length, true);
  }
}