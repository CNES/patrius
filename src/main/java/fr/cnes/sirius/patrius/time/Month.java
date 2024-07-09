/**
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cnes.sirius.patrius.time;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Enumerate representing a calendar month.
 * <p>
 * This enum is mainly useful to parse data files that use month names like Jan or JAN or January or numbers like 1 or
 * 01. It handles month numbers as well as three letters abbreviation and full names, independently of capitalization.
 * </p>
 * 
 * @see DateComponents
 * @author Luc Maisonobe
 */
public enum Month {

    /** January. */
    JANUARY(1),

    /** February. */
    FEBRUARY(2),

    /** March. */
    MARCH(3),

    /** April. */
    APRIL(4),

    /** May. */
    MAY(5),

    /** June. */
    JUNE(6),

    /** July. */
    JULY(7),

    /** August. */
    AUGUST(8),

    /** September. */
    SEPTEMBER(9),

    /** October. */
    OCTOBER(10),

    /** November. */
    NOVEMBER(11),

    /** December. */
    DECEMBER(12);

    /** Parsing map. */
    private static final Map<String, Month> STRINGS_MAP = new ConcurrentHashMap<String, Month>();
    static {
        for (final Month month : values()) {
            STRINGS_MAP.put(month.getLowerCaseName(), month);
            STRINGS_MAP.put(month.getLowerCaseAbbreviation(), month);
        }
    }

    /** Numbers map. */
    private static final Map<Integer, Month> NUMBERS_MAP = new ConcurrentHashMap<Integer, Month>();
    static {
        for (final Month month : values()) {
            NUMBERS_MAP.put(month.getNumber(), month);
        }
    }

    /** Month number. */
    private final int number;

    /** Lower case full name. */
    private final String lowerCaseName;

    /** Capitalized full name. */
    private final String capitalizedName;

    /** Upper case three letters abbreviation. */
    private final String upperCaseAbbreviation;

    /** Lower case three letters abbreviation. */
    private final String lowerCaseAbbreviation;

    /** Capitalized three letters abbreviation. */
    private final String capitalizedAbbreviation;

    /**
     * Simple constructor.
     * 
     * @param numberIn
     *        month number
     */
    private Month(final int numberIn) {
        this.number = numberIn;
        this.lowerCaseName = this.toString().toLowerCase(Locale.getDefault());
        this.capitalizedName = this.toString().charAt(0) + this.lowerCaseName.substring(1);
        this.upperCaseAbbreviation = this.toString().substring(0, 3);
        this.lowerCaseAbbreviation = this.lowerCaseName.substring(0, 3);
        this.capitalizedAbbreviation = this.capitalizedName.substring(0, 3);
    }

    /**
     * Get the month number.
     * 
     * @return month number between 1 and 12
     */
    public int getNumber() {
        return this.number;
    }

    /**
     * Get the upper case full name.
     * 
     * @return upper case full name
     */
    public String getUpperCaseName() {
        return this.toString();
    }

    /**
     * Get the lower case full name.
     * 
     * @return lower case full name
     */
    public String getLowerCaseName() {
        return this.lowerCaseName;
    }

    /**
     * Get the capitalized full name.
     * 
     * @return capitalized full name
     */
    public String getCapitalizedName() {
        return this.capitalizedName;
    }

    /**
     * Get the upper case three letters abbreviation.
     * 
     * @return upper case three letters abbreviation
     */
    public String getUpperCaseAbbreviation() {
        return this.upperCaseAbbreviation;
    }

    /**
     * Get the lower case three letters abbreviation.
     * 
     * @return lower case three letters abbreviation
     */
    public String getLowerCaseAbbreviation() {
        return this.lowerCaseAbbreviation;
    }

    /**
     * Get the capitalized three letters abbreviation.
     * 
     * @return capitalized three letters abbreviation
     */
    public String getCapitalizedAbbreviation() {
        return this.capitalizedAbbreviation;
    }

    /**
     * Parse the string to get the month.
     * <p>
     * The string can be either the month number, the full name or the three letter abbreviation. The parsing ignore the
     * case of the specified string and trims surrounding blanks.
     * </p>
     * 
     * @param s
     *        string to parse
     * @return the month corresponding to the string
     * @exception IllegalArgumentException
     *            if the string does not correspond to a month
     */
    @SuppressWarnings("PMD.PreserveStackTrace")
    public static Month parseMonth(final String s) {
        final String normalizedString = s.trim().toLowerCase(Locale.getDefault());
        final Month month = STRINGS_MAP.get(normalizedString);
        if (month == null) {
            try {
                return getMonth(Integer.parseInt(normalizedString));
            } catch (final NumberFormatException nfe) {
                throw PatriusException.createIllegalArgumentException(PatriusMessages.UNKNOWN_MONTH, s);
            }
        }
        return month;
    }

    /**
     * Get the month corresponding to a number.
     * 
     * @param number
     *        month number
     * @return the month corresponding to the string
     * @exception IllegalArgumentException
     *            if the string does not correspond to a month
     */
    public static Month getMonth(final int number) {
        final Month month = NUMBERS_MAP.get(number);
        if (month == null) {
            throw PatriusException.createIllegalArgumentException(PatriusMessages.UNKNOWN_MONTH, number);
        }
        return month;
    }

}
