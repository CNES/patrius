/**
 * Copyright 2002-2012 CS Systèmes d'Information
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
 */
/*
 *
 * HISTORY
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:144:31/10/2013:Changed UT1-UTC to UT1-TAI
 * END-HISTORY
 *
 */
package fr.cnes.sirius.patrius.frames.configuration.eop;

import java.io.Serializable;
import java.util.Date;

import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.time.TimeStamped;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class holds an Earth Orientation Parameters entry.
 * 
 * @author Luc Maisonobe
 */
public class EOPEntry implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -3150843129484837222L;

    /** Date reference. */
    private static final int DATE_REF = 40587;

    /** 1000 days in seconds. */
    private static final long ONE_THOUSAND_DAYS = 86400000L;

    /** Entry date (absolute date). */
    private final AbsoluteDate date;

    /** UT1-TAI. */
    private final double dt;

    /** Length of day. */
    private final double lod;

    /** X component of pole motion. */
    private final double x;

    /** Y component of pole motion. */
    private final double y;

    /** nutation correction for the X component of pole motion (IAU 2000) or longitude offset (IAU 1980). */
    private final double dx;

    /** nutation correction for the Y component of pole motion (IAU 2000) or obliquity offset (IAU 1980). */
    private final double dy;

    /**
     * dt type.
     */
    public enum DtType {
        /** dt represents UT1-TAI. */
        UT1_TAI,
        /** dt represents UT1-UTC. */
        UT1_UTC;
    }

    /**
     * Simple constructor.
     * 
     * @param mjd
     *        entry date (modified julian day, 00h00 UTC scale)
     * @param dtIn
     *        UT1-UTC in seconds
     * @param lodIn
     *        length of day
     * @param xIn
     *        X component of pole motion
     * @param yIn
     *        Y component of pole motion
     * @param dxIn
     *        nutation correction for the X component (IAU 2000) or longitude offset (IAU 1980)
     * @param dyIn
     *        nutation correction for the Y component (IAU 2000) or obliquity offset (IAU 1980)
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOPEntry(final int mjd, final double dtIn, final double lodIn,
        final double xIn, final double yIn, final double dxIn, final double dyIn) throws PatriusException {

        // convert mjd date at 00h00 UTC to absolute date
        this(new AbsoluteDate(new Date((mjd - DATE_REF) * ONE_THOUSAND_DAYS),
            TimeScalesFactory.getUTC()), dtIn, lodIn, xIn, yIn, dxIn, dyIn, DtType.UT1_UTC);

    }

    /**
     * Constructor with DateComponents parameter.
     * 
     * @param datec
     *        a DateComponents instance
     * @param dtIn
     *        UT1-UTC in seconds
     * @param lodIn
     *        length of day
     * @param xIn
     *        X component of pole motion
     * @param yIn
     *        Y component of pole motion
     * @param dxIn
     *        nutation correction for the X component (IAU 2000) or longitude offset (IAU 1980)
     * @param dyIn
     *        nutation correction for the Y component (IAU 2000) or obliquity offset (IAU 1980)
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOPEntry(final DateComponents datec, final double dtIn, final double lodIn,
        final double xIn, final double yIn, final double dxIn, final double dyIn) throws PatriusException {

        // convert to absolute date at 00h00 UTC
        this(new AbsoluteDate(datec, TimeScalesFactory.getUTC()), dtIn, lodIn, xIn, yIn, dxIn, dyIn, DtType.UT1_UTC);

    }

    /**
     * Constructor with an AbsoluteDate parameter.
     * 
     * @param adate
     *        an AbsoluteDate instance
     * @param dtIn
     *        UT1-UTC in seconds
     * @param lodIn
     *        length of day
     * @param xIn
     *        X component of pole motion
     * @param yIn
     *        Y component of pole motion
     * @param dxIn
     *        nutation correction for the X component (IAU 2000) or longitude offset (IAU 1980)
     * @param dyIn
     *        nutation correction for the Y component (IAU 2000) or obliquity offset (IAU 1980)
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOPEntry(final AbsoluteDate adate, final double dtIn, final double lodIn,
        final double xIn, final double yIn, final double dxIn, final double dyIn) throws PatriusException {
        this(adate, dtIn, lodIn, xIn, yIn, dxIn, dyIn, DtType.UT1_UTC);
    }

    /**
     * Simple constructor.
     * 
     * @param mjd
     *        entry date (modified julian day, 00h00 UTC scale)
     * @param dtIn
     *        UT1-UTC or UT1-TAI in seconds (see param type)
     * @param lodIn
     *        length of day
     * @param xIn
     *        X component of pole motion
     * @param yIn
     *        Y component of pole motion
     * @param dxIn
     *        nutation correction for the X component (IAU 2000) or longitude offset (IAU 1980)
     * @param dyIn
     *        nutation correction for the Y component (IAU 2000) or obliquity offset (IAU 1980)
     * @param type
     *        dt type : UT1-TAI or UT1-UTC
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOPEntry(final int mjd, final double dtIn, final double lodIn,
        final double xIn, final double yIn, final double dxIn, final double dyIn,
        final DtType type) throws PatriusException {

        // convert mjd date at 00h00 UTC to absolute date
        this(new AbsoluteDate(new Date((mjd - DATE_REF) * ONE_THOUSAND_DAYS), TimeScalesFactory.getUTC()),
            dtIn, lodIn, xIn, yIn, dxIn, dyIn, type);
    }

    /**
     * Constructor with DateComponents parameter.
     * 
     * @param datec
     *        a DateComponents instance
     * @param dtIn
     *        UT1-UTC or UT1-TAI in seconds (see param type)
     * @param lodIn
     *        length of day
     * @param xIn
     *        X component of pole motion
     * @param yIn
     *        Y component of pole motion
     * @param dxIn
     *        nutation correction for the X component (IAU 2000) or longitude offset (IAU 1980)
     * @param dyIn
     *        nutation correction for the Y component (IAU 2000) or obliquity offset (IAU 1980)
     * @param type
     *        dt type : UT1-TAI or UT1-UTC
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOPEntry(final DateComponents datec, final double dtIn, final double lodIn,
        final double xIn, final double yIn, final double dxIn, final double dyIn,
        final DtType type) throws PatriusException {

        // convert to absolute date at 00h00 UTC
        this(new AbsoluteDate(datec, TimeScalesFactory.getUTC()), dtIn, lodIn, xIn, yIn, dxIn, dyIn, type);

    }

    /**
     * Constructor with an AbsoluteDate parameter.
     * 
     * @param adate
     *        an AbsoluteDate instance
     * @param dtIn
     *        UT1-UTC or UT1-TAI in seconds (see param type)
     * @param lodIn
     *        length of day
     * @param xIn
     *        X component of pole motion
     * @param yIn
     *        Y component of pole motion
     * @param dxIn
     *        nutation correction for the X component (IAU 2000) or longitude offset (IAU 1980)
     * @param dyIn
     *        nutation correction for the Y component (IAU 2000) or obliquity offset (IAU 1980)
     * @param type
     *        dt type : UT1-TAI or UT1-UTC
     * @exception PatriusException
     *            if UTC time scale cannot be retrieved
     */
    public EOPEntry(final AbsoluteDate adate, final double dtIn, final double lodIn,
        final double xIn, final double yIn, final double dxIn, final double dyIn,
        final DtType type) throws PatriusException {
        // attributes as given
        this.date = adate;
        switch (type) {
            case UT1_TAI:
                this.dt = dtIn;
                break;
            default:
                this.dt = dtIn + TimeScalesFactory.getUTC().offsetFromTAI(adate);
                break;
        }
        this.lod = lodIn;
        this.x = xIn;
        this.y = yIn;
        this.dx = dxIn;
        this.dy = dyIn;
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return this.date;
    }

    /**
     * Get the UT1-TAI value.
     * 
     * @return UT1-TAI in seconds
     */
    public double getUT1MinusTAI() {
        return this.dt;
    }

    /**
     * Get the LoD (Length of Day) value.
     * 
     * @return LoD in seconds
     */
    public double getLOD() {
        return this.lod;
    }

    /**
     * Get the X component of the pole motion.
     * 
     * @return X component of pole motion
     */
    public double getX() {
        return this.x;
    }

    /**
     * Get the Y component of the pole motion.
     * 
     * @return Y component of pole motion
     */
    public double getY() {
        return this.y;
    }

    /**
     * Get the dx correction of the X component of the celestial pole (IAU 2000) or celestial pole offset in longitude
     * (IAU 1980).
     * 
     * @return &delta;&Delta;&psi;<sub>1980</sub> parameter (radians) or &delta;X<sub>2000</sub> parameter (radians)
     */
    public double getDX() {
        return this.dx;
    }

    /**
     * Get the dy correction of the Y component of the celestial pole (IAU 2000) or celestial pole offset in obliquity
     * (IAU 1980).
     * 
     * @return &delta;&Delta;&epsilon;<sub>1980</sub> parameter (radians) or &delta;Y<sub>2000</sub> parameter (radians)
     */
    public double getDY() {
        return this.dy;
    }
}
