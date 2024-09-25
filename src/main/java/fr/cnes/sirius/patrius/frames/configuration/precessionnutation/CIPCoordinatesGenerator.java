/**
 * Copyright 2011-2022 CNES
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
 * @history creation 11/10/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-103:08/12/2023:[PATRIUS] Optimisation du CIRFProvider
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:287:21/10/2014:Bug in frame transformation when changing order of two following transformation
 * VERSION::FA:410:16/04/2015: Anomalies in the Patrius Javadoc
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.precessionnutation;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TAIScale;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStampedGenerator;

/**
 * The class generates {@link CIPCoordinates} to be used independently or within a
 * {@link fr.cnes.sirius.patrius.time.TimeStampedCache}. The method applied is that of the IAU-2000.
 *
 * @see PrecessionNutationModel
 * @author Rami Houdroge
 * @version $Id: CIPCoordinatesGenerator.java 18073 2017-10-02 16:48:07Z bignon $
 * @since 1.3
 */
public class CIPCoordinatesGenerator implements TimeStampedGenerator<CIPCoordinates> {

     /** Serializable UID. */
    private static final long serialVersionUID = -2822151463316105515L;

    /** 0.5. */
    private static final double HALF = 0.5;

    /** Interpolation points. */
    private final int interpolationPoints;

    /** Time span between each data point. */
    private final double span;

    /** Model. */
    private final PrecessionNutationModel model;

    /**
     * Constructor.
     *
     * @param pnModel
     *        Precession Nutation model to use
     * @param interpolationPointsIn
     *        indicates how many points to generate when the {@link #generate(CIPCoordinates, AbsoluteDate)} method
     *        is called upon by the {@link fr.cnes.sirius.patrius.time.TimeStampedCache}.
     * @param spanIn
     *        Time span between generated reference points
     */
    protected CIPCoordinatesGenerator(final PrecessionNutationModel pnModel, final int interpolationPointsIn,
        final double spanIn) {
        this.model = pnModel;
        this.interpolationPoints = interpolationPointsIn;
        this.span = spanIn;
    }

    /** {@inheritDoc} */
    @Override
    public List<CIPCoordinates> generate(final CIPCoordinates existingData, final AbsoluteDate date) {

        // Generate CIP coordinates container
        final List<CIPCoordinates> data = new ArrayList<>();

        // if existing data is null, create one that covers (interpolationPoints / 2) data points
        final CIPCoordinates existing;
        if (existingData == null) {

            final TAIScale tai = TimeScalesFactory.getTAI();
            // find the integer representing the nearest number of spans for the current hour in the day:
            final long t = MathLib.round(date.getComponents(tai).getTime().getSecondsInDay() / this.span);
            // get the current year/month/day, add t * span, shift this date backwards (0.5 * interpolationPoints) *
            // span:
            final AbsoluteDate newDate = new AbsoluteDate(date.getComponents(tai).getDate(),
                new TimeComponents(0, 0, 0), tai).shiftedBy(this.span * (t - HALF * this.interpolationPoints));

            existing = this.model.getCIPCoordinates(newDate);
            data.add(existing);
        } else {
            existing = existingData;
        }

        // if data is requested at a date prior to the closest point, create sets backwards covering dates from existing
        // data
        // to user data - (interpolationPoints / 2) data points ((interpolationPoints / 2) days). Otherwise, generate
        // data forward.
        if (existing.getDate().compareTo(date) > 0) {

            final TreeSet<AbsoluteDate> dates = new TreeSet<>();

            // starting date
            AbsoluteDate current = existing.getDate();

            // generate backwards
            while (current.durationFrom(date) >= -this.span * this.interpolationPoints / 2) {
                current = current.shiftedBy(-this.span);
                dates.add(current);
            }

            // add to list
            for (final AbsoluteDate currentDate : dates) {
                data.add(this.model.getCIPCoordinates(currentDate));
            }

        } else if (existing.getDate().compareTo(date) <= 0) {

            // starting date
            AbsoluteDate current = existing.getDate();

            // generate forward
            while (current.durationFrom(date) <= this.span * this.interpolationPoints / 2) {
                current = current.shiftedBy(this.span);
                data.add(this.model.getCIPCoordinates(current));
            }
        }

        return data;
    }
}
