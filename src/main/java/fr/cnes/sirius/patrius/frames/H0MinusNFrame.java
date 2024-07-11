/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:661:01/02/2017:add H0MinusNFrame class
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames;

import fr.cnes.sirius.patrius.frames.transformations.H0MinusNProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * "H0 - n" reference frame.
 * </p>
 * The "H0 - n" frame is a pseudo-inertial frame, built from the GCRF-ITRF transformation at the date H0 - n; this
 * transformation is "frozen" in time, and it is combined to a rotation of an angle "longitude" around the Z axis
 * of the ITRF frame.
 * <p>
 * Its parent frame is the GCRF frame.
 * </p>
 * 
 * @author Emmanuel Bignon
 * @version $Id: H0MinusNFrame.java 18082 2017-10-02 16:54:17Z bignon $
 * @since 3.4
 */
public class H0MinusNFrame extends Frame {

    /** Serial UID. */
    private static final long serialVersionUID = -7728736096497526402L;

    /** Reference date. */
    private final AbsoluteDate h0;

    /** Reference date shift. */
    private final double n;

    /** The rotation angle around the ITRF Z axis. */
    private final double longitude;

    /**
     * Constructor.
     * 
     * @param name
     *        frame name
     * @param h0In
     *        reference date
     * @param nIn
     *        reference date shift
     * @param longitudeIn
     *        rotation angle around the ITRF Z axis (rad)
     * @throws PatriusException
     *         thrown if the ITRF-GCRF transformation cannot be computed
     */
    public H0MinusNFrame(final String name, final AbsoluteDate h0In, final double nIn,
        final double longitudeIn) throws PatriusException {
        super(FramesFactory.getGCRF(), new H0MinusNProvider(h0In.shiftedBy(-nIn), longitudeIn), name, true);
        this.h0 = h0In;
        this.n = nIn;
        this.longitude = longitudeIn;
    }

    /**
     * Getter for the reference date.
     * 
     * @return the reference date
     */
    public final AbsoluteDate getH0() {
        return this.h0;
    }

    /**
     * Getter for the reference date shift.
     * 
     * @return the reference date shift
     */
    public final double getN() {
        return this.n;
    }

    /**
     * Getter for the rotation angle around the ITRF Z axis.
     * 
     * @return the rotation angle around the ITRF Z axis (rad)
     */
    public final double getLongitude() {
        return this.longitude;
    }
}
