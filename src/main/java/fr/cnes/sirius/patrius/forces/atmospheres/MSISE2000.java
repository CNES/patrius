/* $Id: NRLMSISE00.java 17582 2017-05-10 12:58:16Z bignon $
 * =============================================================
 * Copyright (c) CNES 2010
 * This software is part of STELA, a CNES tool for long term
 * orbit propagation. This source file is licensed as described
 * in the file LICENCE which is part of this distribution
 * =============================================================
 */
/**
 *
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
 * @history Created 25/04/2012
 *
 * HISTORY
 * VERSION:4.13:DM:DM-70:08/12/2023:[PATRIUS] Calcul de jacobienne dans OneAxisEllipsoid
 * VERSION:4.13:DM:DM-3:08/12/2023:[PATRIUS] Distinction entre corps celestes et barycentres
 * VERSION:4.12.1:FA:FA-123:05/09/2023:[PATRIUS] Utilisation de getLLHCoordinates() au
 * lieu de getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC)
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:FA:FA-2741:27/01/2021:[PATRIUS] Chaine de transformation de repere non optimale dans MSIS2000
 * VERSION:4.5:FA:FA-2446:27/05/2020:optimisation de l'utilisation du cache dans MSISE2000 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:130:08/10/2013:MSIS2000 model update
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::FA:576:22/03/2016:cache mechanism for density
 * VERSION::FA:594:05/04/2016:computation time optimisation
 * VERSION::DM:606:05/08/2016:extended atmosphere data
 * VERSION::FA:575:29/03/2017:add warning about MSIS discontinuity
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::FA:1196:15/11/2017:add getPressure() method
 * VERSION::FA:1486:18/05/2018:modify the hydrogen mass unity to USI and add precision
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.forces.atmospheres;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.CelestialPoint;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidPoint;
import fr.cnes.sirius.patrius.bodies.LLHCoordinatesSystem;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.ApCoef;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.Flags;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.Input;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.NRLMSISE00;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.Output;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class implements the MSIS00 atmospheric model. <br>
 * It is an interface layer between the {@link NRLMSISE00} class - adapted from Fortran - and the SIRIUS data
 * structures.
 *
 * <p>
 * <b>Warning</b>: this model is not continuous. There is a discontinuity every day (at 0h in UTC time scale).
 * Discontinuities are however very small (1E-3 on a relative scale).
 * </p>
 * <p>
 * This class is restricted to be used with {@link EllipsoidBodyShape}.
 * </p>
 *
 * @concurrency thread-hostile
 * @concurrency.comment The direct use of thread hostile objects makes this class thread hostile itself.
 *
 * @author Vincent Ruch, Rami Houdroge
 * @since 1.2
 * @version $Id: MSISE2000.java 17794 2017-09-01 07:15:56Z bignon $
 */
@SuppressWarnings("PMD.NullAssignment")
public class MSISE2000 implements ExtendedAtmosphere {

    /** Serializable UID. */
    private static final long serialVersionUID = -7842070913367047706L;

    /**
     * Milliseconds from CNES julian day to January 1, 1970 00:00:00 GMT.
     *
     * @see Date#setTime(long)
     */
    private static final double CNESJD_MS_TO_1970EPOCH = -631152000000.;

    /** Seconds in one day. */
    private static final double SECONDS_IN_DAY = 86400;

    /** Milliseconds in one day. */
    private static final double MS_IN_DAY = SECONDS_IN_DAY * 1000;

    /** km to m conversion */
    private static final double KM_TO_M = 1000;

    /** Specific gaz constant for air J*kg<sup>-1</sup>*K<sup>-1</sup>. */
    private static final double R = 287.058;

    /** GM/CM<sup>3</sup> to KG/M<sup>3</sup> conversion. */
    private static final double G_PER_CM3_IN_KG_PER_M3 = 1000;

    /** Hours in one day. */
    private static final double HOURS_IN_DAY = 24;

    /** Adiabatic constant. */
    private static final double GAMMA = 1.4;

    /** Flag size. */
    private static final int FLAGSIZE = 24;

    /** Specific switch index. */
    private static final int SWITCHINDEX = 9;

    /** Index for output 7. */
    private static final int OUTPUT7 = 7;

    /** Index for output 8. */
    private static final int OUTPUT8 = 8;

    /** An instance of the MSIS00 model. */
    private final NRLMSISE00 atmosModel;

    /** Flags for the MSIS00 density computation. */
    private final Flags flags;

    /** Earth Shape. */
    private final EllipsoidBodyShape earth;

    /** Sun. */
    private final CelestialPoint sun;

    /** Solar activity data container. */
    private final MSISE2000InputParameters inputParams;

    /** Calendar (internal variable, set there for computation time saving). */
    private final Calendar calendar;

    /** Cache mecanism - Output pressure. */
    private double cachedPressure;

    /** Cache mecanism - Output temperature. */
    private double cachedTemperature;

    /** Cache mecanism - Output atmosphere data. */
    private transient AtmosphereData cachedOutputData;

    /** Cache mecanism - Input date. */
    private AbsoluteDate cachedDate;

    /** Cache mecanism - Input frame. */
    private Frame cachedFrame;

    /** Cache mecanism - Input position. */
    private Vector3D cachedPosition;

    /**
     * Simple constructor for class MSIS00Adapter.
     *
     * @param data
     *        solar data
     * @param earthBody
     *        earth body
     * @param sun
     *        the sun
     */
    public MSISE2000(final MSISE2000InputParameters data, final EllipsoidBodyShape earthBody,
                     final CelestialPoint sun) {

        this.earth = earthBody;
        this.sun = sun;
        this.inputParams = data;
        this.atmosModel = new NRLMSISE00();
        this.flags = new Flags();
        this.flags.setSwitches(0, 0);
        for (int i = 1; i < FLAGSIZE; i++) {
            this.flags.setSwitches(i, 1);
        }
        this.flags.setSwitches(SWITCHINDEX, -1);

        // Cache initialization
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
        this.cachedFrame = null;
        this.cachedPosition = Vector3D.ZERO;
        this.cachedOutputData = null;

        this.calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT_0"));
    }

    /** {@inheritDoc} */
    @Override
    public double getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        computeTempPresDensity(date, position, frame);
        return this.cachedOutputData.getDensity();
    }

    /**
     * Getter for the pressure.
     *
     * @param date
     *        date
     * @param position
     *        position
     * @param frame
     *        frame
     * @return pressure at date
     * @throws PatriusException
     *         if pressure could not be computed
     */
    public double getPressure(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        computeTempPresDensity(date, position, frame);
        return this.cachedPressure;
    }

    /**
     * If input parameters are different from cached parameters, re compute cached density and temperature.
     *
     * @param date
     *        current date
     * @param position
     *        current position in frame
     * @param frame
     *        the frame in which is defined the position
     * @throws PatriusException
     *         if date is out of range of solar activity model or if some frame conversion cannot be performed
     */
    private void computeTempPresDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {

        if (this.cachedDate.compareTo(date) != 0 || !position.equals(this.cachedPosition)
                || !frame.equals(this.cachedFrame)) {
            final Frame cmod = FramesFactory.getMOD(false);
            final Transform frameToCMOD = frame.getTransformTo(cmod, date);
            final Vector3D cmodPosition = frameToCMOD.transformPosition(position);

            final double jdCNES = getCNESJd(date);

            final EllipsoidPoint point = this.earth.buildPoint(position, frame, date, "satPoint");
            // get latitude of user point:
            final double latitude = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLatitude();
            // get longitude of user point:
            final double longitude = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getLongitude();
            // get altitude of user point:
            final double altitude = point.getLLHCoordinates(LLHCoordinatesSystem.ELLIPSODETIC).getHeight();

            // compute local solar time
            final double resTLoc = computeTLoc(date, cmodPosition);

            // used to get input object day, year and sec
            final Date newDate = new Date();
            newDate.setTime(MathLib.round(jdCNES * MS_IN_DAY + CNESJD_MS_TO_1970EPOCH));
            this.calendar.setTime(newDate);

            /*
             * Provide inputs to NRLMSIS00 class
             */
            final int day = this.calendar.get(Calendar.DAY_OF_YEAR);
            final double sec = (jdCNES - (int) jdCNES) * SECONDS_IN_DAY;
            final double alt = altitude / KM_TO_M;
            final double gLat = MathLib.toDegrees(latitude);
            final double gLong = MathLib.toDegrees(longitude);
            final double lst = resTLoc;
            final double f107 = this.inputParams.getInstantFlux(date);
            final double f107A = this.inputParams.getMeanFlux(date);
            final ApCoef apc = new ApCoef(this.inputParams.getApValues(date));

            final Input input = new Input();
            input.setDoy(day);
            input.setSec(sec);
            input.setAlt(alt);
            input.setgLat(gLat);
            input.setgLong(gLong);
            input.setLst(lst);
            input.setF107(f107);
            input.setF107A(f107A);
            input.setAp(0);
            input.setApA(apc);
            // density computation
            final Output output = new Output();
            this.atmosModel.gtd7d(input, this.flags, output);
            final double density = output.getD(5) * G_PER_CM3_IN_KG_PER_M3;
            this.cachedTemperature = output.getT(1);
            if (Double.isNaN(density) || Double.isInfinite(density)) {
                throw new IllegalArgumentException();
            }

            // Build data (number of particules are per cm3 and should be given per m3 but this does
            // not matter
            // since only the relative percentage only matters)
            this.cachedOutputData = new AtmosphereData(density, output.getT(1), output.getT(0), output.getD(0),
                output.getD(1), output.getD(2), output.getD(3), output.getD(4), output.getD(6), output.getD(OUTPUT7),
                output.getD(OUTPUT8));

            // Compute pressure
            final double molarMass = this.cachedOutputData.getMeanAtomicMass() * Constants.AVOGADRO_CONSTANT
                    * AtmosphereData.HYDROGEN_MASS;

            this.cachedPressure = Constants.PERFECT_GAS_CONSTANT * this.cachedTemperature
                    * this.cachedOutputData.getDensity() / molarMass;

            // store input params used to compute these results in cache
            this.cachedDate = date;
            this.cachedPosition = position;
            this.cachedFrame = frame;
        }
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {

        final Transform bodyToFrame = this.earth.getBodyFrame().getTransformTo(frame, date);
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        computeTempPresDensity(date, position, frame);
        return MathLib.sqrt(GAMMA * R * this.cachedTemperature);
    }

    /**
     * Computes the CNES Julian Date corresponding to and {@link AbsoluteDate} <br>
     * CNES JD is defined as Julian days elapsed since 01/01/1950 00:00:00 UTC
     *
     * @param date
     *        date to convert
     * @return CNES julian day
     * @throws PatriusException
     *         if failed to load TAI-TAI history
     */
    private static double getCNESJd(final AbsoluteDate date) throws PatriusException {

        // Reference epoch
        final AbsoluteDate ref = new AbsoluteDate(new DateTimeComponents(DateComponents.FIFTIES_EPOCH,
            TimeComponents.H00), TimeScalesFactory.getUTC());

        // Return offset from reference epoch in days
        return date.offsetFrom(ref, TimeScalesFactory.getUTC()) / SECONDS_IN_DAY;
    }

    /**
     * Computes the local solar time
     *
     * @param date
     *        date
     * @param position
     *        position in Mean of Date Frame
     * @return local solar time
     * @throws PatriusException
     *         it UTC-TAI fails
     */
    private double computeTLoc(final AbsoluteDate date, final Vector3D position) throws PatriusException {

        // compute thetaLST
        final double thetaLST = MathLib.atan2(position.getY(), position.getX());

        // compute thetaSun
        final Vector3D sunP = this.sun.getPVCoordinates(date, FramesFactory.getMOD(false)).getPosition();
        final double thetaSun = MathLib.atan2(sunP.getY(), sunP.getX());

        // local solar time is defined in [0;24[
        double resTLoc = (FastMath.PI + thetaLST - thetaSun) * HOURS_IN_DAY / (2 * FastMath.PI);
        resTLoc = (resTLoc + HOURS_IN_DAY) % HOURS_IN_DAY;

        return resTLoc;
    }

    /**
     * {@inheritDoc}
     * <p>
     * MSISE2000 provides all data mentioned in {@link AtmosphereData}.
     * </p>
     */
    @Override
    public AtmosphereData getData(final AbsoluteDate date, final Vector3D position, final Frame frame)
        throws PatriusException {
        computeTempPresDensity(date, position, frame);
        return this.cachedOutputData;
    }

    /**
     * Getter for the solar parameters.
     *
     * @return the solar parameters
     */
    public MSISE2000InputParameters getParameters() {
        return this.inputParams;
    }

    /**
     * Getter for the earth body.
     *
     * @return the earth body
     */
    public BodyShape getEarthBody() {
        return this.earth;
    }

    /**
     * Getter for the Sun.
     *
     * @return the Sun
     */
    public CelestialPoint getSun() {
        return this.sun;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>inputParams: {@link MSISE2000InputParameters}</li>
     * <li>sun: {@link PVCoordinatesProvider}</li>
     * <li>earth: {@link BodyShape}</li>
     * </ul>
     * </p>
     */
    @Override
    public Atmosphere copy() {
        return new MSISE2000(this.inputParams, this.earth, this.sun);
    }

    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        this.inputParams.checkSolarActivityData(start, end);
    }
}
