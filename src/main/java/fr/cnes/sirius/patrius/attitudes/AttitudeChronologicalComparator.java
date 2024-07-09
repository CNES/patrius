/**
 * 
 * Copyright 2011-2017 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
 * 
 * 
 * @history created 23/04/2013
 * HISTORY
 * VERSION:4.7:DM:DM-2801:18/05/2021:Suppression des classes et methodes depreciees suite au refactoring des slews
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import java.io.Serializable;
import java.util.Comparator;

import fr.cnes.sirius.patrius.math.util.MathLib;

/**
 * This class is a comparator used to compare the Attitude objects in the ephemeris set. This comparators allows two
 * identical attitudes ephemeris to be kept in the set; this feature is important to compute two ephemeris at the
 * attitude transition points.
 * 
 * @concurrency immutable
 * 
 * @see AbstractAttitudeEphemerisGenerator
 * 
 * @author Tiziana Sabatini
 * 
 * @version $Id: AttitudeChronologicalComparator.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.3
 * 
 */
public final class AttitudeChronologicalComparator implements Comparator<Attitude>, Serializable {

    /** Default serial UID. */
    private static final long serialVersionUID = 757983092465215089L;

    /**
     * Compare two Attitude instances.
     * 
     * @param o1
     *        first Attitude instance
     * @param o2
     *        second Attitude instance
     * @return a negative integer or a positive integer as the first instance is before or after the second one. If
     *         the two instances are simultaneous, returns 1 (to avoid deleting attitude instances in the ephemeris
     *         set).
     */
    @Override
    public int compare(final Attitude o1, final Attitude o2) {
        int rez;
        rez = (int) MathLib.signum(o1.getDate().durationFrom(o2.getDate()));
        if (rez == 0) {
            // if the two attitudes are simultaneous, returns 1: the first attitude is considered to
            // be "before" the second attitude in the ephemeris
            rez = 1;
        }
        return rez;
    }
}
