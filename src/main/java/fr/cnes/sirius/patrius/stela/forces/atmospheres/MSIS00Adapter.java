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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2446:27/05/2020:optimisation de l'utilisation du cache dans MSISE2000 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:91:26/07/2013: handeling properly the Day of the year (satLight-like behaviour)
 * VERSION::DM:130:08/10/2013: MSIS2000 model update
 * VERSION::FA:---:11/04/2014:Quality assurance
 * VERSION::DM:317:04/03/2015: STELA integration in CIRF with referential choice (ICRF, CIRF or MOD)
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
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
package fr.cnes.sirius.patrius.stela.forces.atmospheres;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import fr.cnes.sirius.patrius.bodies.CelestialBody;
import fr.cnes.sirius.patrius.forces.atmospheres.Atmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.forces.atmospheres.ExtendedAtmosphere;
import fr.cnes.sirius.patrius.forces.atmospheres.MSISE2000InputParameters;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.ApCoef;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.Flags;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.Input;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.NRLMSISE00;
import fr.cnes.sirius.patrius.forces.atmospheres.MSIS2000.Output;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.stela.JavaMathAdapter;
import fr.cnes.sirius.patrius.stela.bodies.EarthRotation;
import fr.cnes.sirius.patrius.stela.bodies.GeodPosition;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateComponents;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class implements the MSIS00 atmospheric model. <br>
 * It is an interface layer between the {@link NRLMSISE00} class - adapted from Fortran - and the
 * SIRIUS data structures. It implements the method getDensity of the interface Atmosphere, based on
 * the MSIS00 atmospheric model.
 * 
 * <p>
 * <b>Warning</b>: this model is not continuous. There is a discontinuity every day (at 0h in UTC time scale).
 * Discontinuities are however very small (1E-3 on a relative scale).
 * </p>
 * 
 * @concurrency thread-hostile
 * @concurrency.comment The direct use of thread hostile objects makes this class thread hostile
 *                      itself.
 * 
 * @author Vincent Ruch
 * @author Rami Houdroge
 * @author Tiziana Sabatini
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
@SuppressWarnings("PMD.NullAssignment")
public class MSIS00Adapter implements ExtendedAtmosphere {

    /** Serial number UID. */
    private static final long serialVersionUID = -1995857838866326747L;

    /** Milliseconds from CNES julian day to January 1, 1970 00:00:00 GMT. */
    private static final double CNESJD_MS_TO_1970EPOCH = -631152000000.;

    /** Milliseconds in one day. */
    private static final double MS_IN_DAY = Constants.JULIAN_DAY * 1000;

    /** Km to m conversion. */
    private static final double KM_TO_M = 1000;

    /** Adiabatic constant. */
    private static final double GAMMA = 1.4;

    /** Speed of sound at sea level (m/s). */
    private static final double SOS = 287.058;
    
    /** 7 */
    private static final int SEVEN = 7;
    
    /** 8 */
    private static final int EIGHT = 8;
    
    /** 9 */
    private static final int NINE = 9;
    
    /** 24 */
    private static final int TWENTYFOUR = 24;

    /** An instance of the MSIS00 model. */
    private final NRLMSISE00 atmosModel;

    /** Output for the MSIS00 density computation. */
    private final Output output;

    /** Flags for the MSIS00 density computation. */
    private final Flags flags;

    /** Flattening of the Earth. */
    private final double flattening;

    /** Earth Radius. */
    private final double rEquatorial;

    /** Solar activity data container. */
    private final MSISE2000InputParameters inputParams;

    /** Calendar (internal variable, set there for computation time saving). */
    private final Calendar calendar;

    /** The Sun. */
    private final CelestialBody sun;

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
     * @param data solar data
     * @param rEq the Earth equatorial radius
     * @param f the Earth flattening
     * @param sunBody the sun
     */
    public MSIS00Adapter(final MSISE2000InputParameters data, final double rEq, final double f,
        final CelestialBody sunBody) {
        this.cachedDate = AbsoluteDate.PAST_INFINITY;
        this.cachedFrame = null;
        this.cachedPosition = Vector3D.ZERO;
        this.cachedOutputData = null;
        this.cachedTemperature = Double.NaN;
        this.calendar = new GregorianCalendar(TimeZone.getTimeZone("GMT_0"));
        this.rEquatorial = rEq;
        this.flattening = f;
        this.inputParams = data;
        this.atmosModel = new NRLMSISE00();
        this.output = new Output();
        this.flags = new Flags();
        this.flags.setSwitches(0, 0);
        for (int i = 1; i < TWENTYFOUR; i++) {
            this.flags.setSwitches(i, 1);
        }
        this.flags.setSwitches(NINE, -1);
        this.sun = sunBody;
    }

    /** {@inheritDoc} */
    @Override
    public double
            getDensity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                           throws PatriusException {
        this.computeTempPresDensity(date, position, frame);
        return this.cachedOutputData.getDensity();
    }

    /**
     * Returns pressure.
     * 
     * @param date date
     * @param position position
     * @param frame frame
     * @return pressure at date
     * @throws PatriusException thrown if pressure could not be computed
     */
    public double
            getPressure(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                            throws PatriusException {
        this.computeTempPresDensity(date, position, frame);
        return this.cachedPressure;
    }

    /**
     * If input parameters are different from cached parameters, re compute cached density.
     * 
     * @param date current date
     * @param position current position in frame
     * @param frame the frame in which is defined the position
     * @exception PatriusException if date is out of range of solar activity model or if some frame
     *            conversion cannot be performed
     */
    private void computeTempPresDensity(final AbsoluteDate date, final Vector3D position,
                                        final Frame frame) throws PatriusException {
        if (this.cachedDate.compareTo(date) != 0 || !position.equals(this.cachedPosition)
            || !frame.equals(this.cachedFrame)) {
            // CNES jd
            final double jdCNES = this.getCNESJd(date);
            final GeodPosition geodPosition = new GeodPosition(this.rEquatorial, this.flattening);
            final double latitude = geodPosition.getGeodeticLatitude(position);
            double longitude = JavaMathAdapter.mod(
                geodPosition.getGeodeticLongitude(position, date), 2. * FastMath.PI);
            if (longitude > FastMath.PI) {
                // Geodetic longitude has to be within ]-180; 180]
                longitude = longitude - 2. * FastMath.PI;
            }
            final double altitude = geodPosition.getGeodeticAltitude(position);
            final Vector3D positionSun = this.sun.getPVCoordinates(date, frame).getPosition();
            final double tLoc = geodPosition.getTloc(position, positionSun, date);

            // used to get input object day, year and sec
            final Date newDate = new Date();
            newDate.setTime(MathLib.round(jdCNES * MS_IN_DAY + CNESJD_MS_TO_1970EPOCH));
            this.calendar.setTime(newDate);

            /*
             * Pass inputs to NRLMSIS00 class
             */
            final int day = this.calendar.get(Calendar.DAY_OF_YEAR);
            // In order to have the same reference as SatLight
            // if (day >= 365) {
            // day = day - 365;
            // }
            final double sec = (jdCNES - (int) jdCNES) * Constants.JULIAN_DAY;
            final double alt = altitude / KM_TO_M;
            final double gLat = MathLib.toDegrees(latitude);
            final double gLong = MathLib.toDegrees(longitude);
            final double lst = tLoc;
            final double f107 = this.inputParams.getInstantFlux(date);
            final double f107A = this.inputParams.getMeanFlux(date);
            final ApCoef apc = new ApCoef(this.inputParams.getApValues(date));

            // configure input object
            final Input input = new Input();
            // time
            input.setDoy(day);
            input.setSec(sec);
            // location
            input.setAlt(alt);
            input.setgLat(gLat);
            input.setgLong(gLong);
            input.setLst(lst);
            // solar activity
            input.setF107A(f107A);
            input.setF107(f107);
            input.setAp(0);
            input.setApA(apc);

            // density computation
            this.atmosModel.gtd7d(input, this.flags, this.output);
            final double coef = 1000;
            final double density = this.output.getD()[5] * coef;
            this.cachedTemperature = this.output.getT(1);
            // check for errors
            if (Double.isNaN(density) || Double.isInfinite(density)) {
                throw new IllegalArgumentException();
            }

            // Build data (number of particules are per cm3 and should be given per m3 but this does
            // not matter
            // since only the relative percentage only matters)
            this.cachedOutputData =
                new AtmosphereData(density, this.output.getT(1), this.output.getT(0),
                    this.output.getD(0), this.output.getD(1), this.output.getD(2), this.output.getD(3),
                    this.output.getD(4),
                    this.output.getD(6), this.output.getD(SEVEN), this.output.getD(EIGHT));

            // Compute pressure
            final double molarMass = this.cachedOutputData.getMeanAtomicMass()
                * Constants.AVOGADRO_CONSTANT * AtmosphereData.HYDROGEN_MASS;
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
    public Vector3D
            getVelocity(final AbsoluteDate date, final Vector3D position, final Frame frame)
                                                                                            throws PatriusException {
        final double omegaDouble = EarthRotation.getERADerivative(date);
        final Transform bodyToFrame = new Transform(date,
            new Rotation(Vector3D.PLUS_K, omegaDouble));
        final Vector3D posInBody = bodyToFrame.getInverse().transformPosition(position);
        final PVCoordinates pvBody = new PVCoordinates(posInBody, new Vector3D(0, 0, 0));
        final PVCoordinates pvFrame = bodyToFrame.transformPVCoordinates(pvBody);
        return pvFrame.getVelocity();
    }

    /** {@inheritDoc} */
    @Override
    public double getSpeedOfSound(final AbsoluteDate date, final Vector3D position,
                                  final Frame frame) throws PatriusException {
        this.getDensity(date, position, frame);
        return MathLib.sqrt(GAMMA * SOS * this.cachedTemperature);
    }

    /**
     * Computes the CNES Julian Date corresponding to and {@link AbsoluteDate} <br>
     * CNES JD is defined as Julian days elapsed since 01/01/1950 00:00:00 UTC
     * 
     * @param date date to convert
     * @return CNES julian day
     * @throws PatriusException if failed to load TAI-TAI history
     */
    private double getCNESJd(final AbsoluteDate date) throws PatriusException {

        // Reference epoch
        final AbsoluteDate ref = new AbsoluteDate(new DateTimeComponents(
            DateComponents.FIFTIES_EPOCH, TimeComponents.H00), TimeScalesFactory.getUTC());

        // Return offset from reference epoch in days
        return date.offsetFrom(ref, TimeScalesFactory.getUTC()) / Constants.JULIAN_DAY;
    }

    /**
     * {@inheritDoc}
     * <p>
     * MSISE00Adapter provides all data mentioned in {@link AtmosphereData}.
     * </p>
     */
    @Override
    public AtmosphereData getData(final AbsoluteDate date, final Vector3D position,
                                  final Frame frame) throws PatriusException {
        this.getDensity(date, position, frame);
        return this.cachedOutputData;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>inputParams: {@link MSISE2000InputParameters}</li>
     * <li>sun: {@link PVCoordinatesProvider}</li>
     * </ul>
     * </p>
     */
    @Override
    public Atmosphere copy() {
        return new MSIS00Adapter(this.inputParams, this.rEquatorial, this.flattening, this.sun);
    }
    
    /** {@inheritDoc} */
    @Override
    public void checkSolarActivityData(final AbsoluteDate start, final AbsoluteDate end) throws PatriusException {
        inputParams.checkSolarActivityData(start, end);
    }
}
