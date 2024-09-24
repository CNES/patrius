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
 * @history creation 12/10/2012
 *
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:06/11/2013:Changed UT1-UTC correction to UT1-TAI correction
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.tides;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.TimeScale;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStampedGenerator;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.TimeStampedCacheException;

/**
 * Tidal corrections generator for the TimeStampedCache.
 * 
 * @author Rami Houdroge
 * @see TidalCorrectionCache
 * @version $Id: TidalCorrectionGenerator.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 * @since 1.3
 */
public class TidalCorrectionGenerator implements TimeStampedGenerator<TidalCorrection> {

     /** Serializable UID. */
    private static final long serialVersionUID = -5855298241537285926L;

    /** Offset from reference epoch (days). */
    private static final double OFFSET = 37076.5;

    /** Time span between reference points (secs). */
    private final double span;

    /** Number of reference points to use for the interpolation. */
    private final int interpolationPoints;

    /** Tidel corrections model. */
    private final TidalCorrectionModel model;

    /**
     * Simple constructor.
     * 
     * @param interpolationPointsIn
     *        number of reference points to use for the interpolation
     * @param spanIn
     *        time span between reference points
     * @param tcModel
     *        Tidal corrections model to use
     */
    protected TidalCorrectionGenerator(final TidalCorrectionModel tcModel, final int interpolationPointsIn,
        final double spanIn) {
        this.interpolationPoints = interpolationPointsIn;
        this.span = spanIn;
        this.model = tcModel;
    }

    /** {@inheritDoc} */
    @Override
    public List<TidalCorrection> generate(final TidalCorrection existingData,
                                          final AbsoluteDate date) throws TimeStampedCacheException {

        // Generate CIP coordinates container
        final List<TidalCorrection> data = new ArrayList<>();

        // / if existing data is null, create one that covers (interpolationPoints / 2) data points before the user
        // specified date
        final TidalCorrection existing;
        if (existingData == null) {

            final TimeScale tai = TimeScalesFactory.getTAI();

            final double t = date.durationFrom(AbsoluteDate.MODIFIED_JULIAN_EPOCH) / Constants.JULIAN_DAY - OFFSET;
            final double tCenter =
                (this.span / Constants.JULIAN_DAY) * MathLib.floor(t / (this.span / Constants.JULIAN_DAY));

            final AbsoluteDate newDate = new AbsoluteDate(AbsoluteDate.MODIFIED_JULIAN_EPOCH, (tCenter + OFFSET) *
                Constants.JULIAN_DAY - this.span * this.interpolationPoints / 2, tai);

            existing =
                new TidalCorrection(newDate, this.model.getPoleCorrection(newDate),
                    this.model.getUT1Correction(newDate),
                    this.model.getLODCorrection(newDate));
            data.add(existing);
        } else {
            existing = existingData;
        }

        // if data is requested at a date prior to the closest point, create sets backwards covering dates from existing
        // data
        // to user data - (interpolationPoints / 2) data points. Otherwise, generate data forward.
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
            for (final AbsoluteDate d : dates) {
                data.add(new TidalCorrection(d, this.model.getPoleCorrection(d), this.model.getUT1Correction(d),
                    this.model
                        .getLODCorrection(d)));
            }

        } else if (existing.getDate().compareTo(date) <= 0) {

            // starting date
            AbsoluteDate current = existing.getDate();

            // generate forward
            while (current.durationFrom(date) <= this.span * this.interpolationPoints / 2) {
                current = current.shiftedBy(this.span);
                data.add(new TidalCorrection(current, this.model.getPoleCorrection(current),
                    this.model.getUT1Correction(current), this.model.getLODCorrection(current)));
            }
        }

        return data;
    }
}
