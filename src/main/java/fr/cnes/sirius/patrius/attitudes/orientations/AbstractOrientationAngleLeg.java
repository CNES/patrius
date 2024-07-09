/**
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
 * HISTORY
* VERSION:4.4:DM:DM-2208:04/10/2019:[PATRIUS] Ameliorations de Leg, LegsSequence et AbstractLegsSequence
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1949:14/11/2018:add new orientation feature
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes.orientations;

import fr.cnes.sirius.patrius.time.AbsoluteDateInterval;

/**
 * This abstract class aims at defining all common features to classes representing the leg of an
 * {@link OrientationAngleLeg}.
 *
 * @author Miguel Morère
 *
 * @since 4.2
 */
public abstract class AbstractOrientationAngleLeg implements OrientationAngleLeg {

    /** Serial ID. */
    private static final long serialVersionUID = -2620428225775307446L;

    /** Default nature. */
    private static final String ORIENTATION_ANGLE_LEG = "ORIENTATION_ANGLE_LEG";

    /** Time interval. */
    private AbsoluteDateInterval timeInterval;

    /** Nature. */
    private final String nature;

    /**
     * Constructor.
     * 
     * @param timeIntervalIn leg time interval
     * @param natureIn leg nature
     */
    public AbstractOrientationAngleLeg(final AbsoluteDateInterval timeIntervalIn,
        final String natureIn) {
        this.timeInterval = timeIntervalIn;
        this.nature = natureIn;
    }

    /**
     * Constructor with default value for the leg nature.
     * 
     * @param timeIntervalIn leg time interval
     */
    public AbstractOrientationAngleLeg(final AbsoluteDateInterval timeIntervalIn) {
        this(timeIntervalIn, ORIENTATION_ANGLE_LEG);
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDateInterval getTimeInterval() {
        return this.timeInterval;
    }

    /** {@inheritDoc} */
    @Override
    public String getNature() {
        return this.nature;
    }
}
