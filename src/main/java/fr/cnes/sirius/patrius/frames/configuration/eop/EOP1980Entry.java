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
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:31/10/2013:Changed UT1-UTC to UT1-TAI
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class holds an Earth Orientation Parameters entry (IAU1980).
 * 
 * @author Luc Maisonobe
 */
public class EOP1980Entry extends EOPEntry {

    /** Serializable UID. */
    private static final long serialVersionUID = -8820663105212993131L;

    /**
     * Simple constructor.
     * 
     * @param mjd
     *        entry date (modified julian day, 00h00 UTC scale)
     * @param dt
     *        UT1-UTC or UT1-TAI in seconds (see parameter type)
     * @param lod
     *        length of day
     * @param x
     *        X component of pole motion
     * @param y
     *        Y component of pole motion
     * @param ddPsi
     *        correction for nutation in longitude
     * @param ddEps
     *        correction for nutation in obliquity
     * @param type
     *        type for dt (UT1-UTC or UT1-TAI)
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOP1980Entry(final int mjd, final double dt, final double lod,
        final double x, final double y,
        final double ddPsi, final double ddEps, final DtType type) throws PatriusException {
        super(mjd, dt, lod, x, y, ddPsi, ddEps, type);
    }

    /**
     * Constructor with DateComponents parameter.
     * 
     * @param datec
     *        a DateComponents instance
     * @param dt
     *        UT1-UTC or UT1-TAI in seconds (see parameter type)
     * @param lod
     *        length of day
     * @param x
     *        X component of pole motion
     * @param y
     *        Y component of pole motion
     * @param ddPsi
     *        correction for nutation in longitude
     * @param ddEps
     *        correction for nutation in obliquity
     * @param type
     *        type for dt (UT1-UTC or UT1-TAI)
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOP1980Entry(final DateComponents datec, final double dt, final double lod,
        final double x, final double y,
        final double ddPsi, final double ddEps, final DtType type) throws PatriusException {
        super(datec, dt, lod, x, y, ddPsi, ddEps, type);
    }

    /**
     * Constructor with an AbsoluteDate parameter.
     * 
     * @param adate
     *        an AbsoluteDate instance
     * @param dt
     *        UT1-UTC or UT1-TAI in seconds (see parameter type)
     * @param lod
     *        length of day
     * @param x
     *        X component of pole motion
     * @param y
     *        Y component of pole motion
     * @param ddPsi
     *        correction for nutation in longitude
     * @param ddEps
     *        correction for nutation in obliquity
     * @param type
     *        type for dt (UT1-UTC or UT1-TAI)
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOP1980Entry(final AbsoluteDate adate, final double dt, final double lod,
        final double x, final double y,
        final double ddPsi, final double ddEps, final DtType type) throws PatriusException {
        super(adate, dt, lod, x, y, ddPsi, ddEps, type);
    }

    /**
     * Simple constructor.
     * 
     * @param mjd
     *        entry date (modified julian day, 00h00 UTC scale)
     * @param dt
     *        UT1-UTC in seconds
     * @param lod
     *        length of day
     * @param x
     *        X component of pole motion
     * @param y
     *        Y component of pole motion
     * @param ddPsi
     *        correction for nutation in longitude
     * @param ddEps
     *        correction for nutation in obliquity
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOP1980Entry(final int mjd, final double dt, final double lod,
        final double x, final double y,
        final double ddPsi, final double ddEps) throws PatriusException {
        super(mjd, dt, lod, x, y, ddPsi, ddEps);
    }

    /**
     * Constructor with DateComponents parameter.
     * 
     * @param datec
     *        a DateComponents instance
     * @param dt
     *        UT1-UTC in seconds
     * @param lod
     *        length of day
     * @param x
     *        X component of pole motion
     * @param y
     *        Y component of pole motion
     * @param ddPsi
     *        correction for nutation in longitude
     * @param ddEps
     *        correction for nutation in obliquity
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOP1980Entry(final DateComponents datec, final double dt, final double lod,
        final double x, final double y,
        final double ddPsi, final double ddEps) throws PatriusException {
        super(datec, dt, lod, x, y, ddPsi, ddEps);
    }

    /**
     * Constructor with an AbsoluteDate parameter.
     * 
     * @param adate
     *        an AbsoluteDate instance
     * @param dt
     *        UT1-UTC in seconds
     * @param lod
     *        length of day
     * @param x
     *        X component of pole motion
     * @param y
     *        Y component of pole motion
     * @param ddPsi
     *        correction for nutation in longitude
     * @param ddEps
     *        correction for nutation in obliquity
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOP1980Entry(final AbsoluteDate adate, final double dt, final double lod,
        final double x, final double y, final double ddPsi, final double ddEps) throws PatriusException {
        super(adate, dt, lod, x, y, ddPsi, ddEps);
    }

}
