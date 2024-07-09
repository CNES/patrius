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
 * @history creation 18/09/2012
 * 
 * HISTORY
* VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: refactoring and renaming of the class
 * VERSION::FA:---:11/04/2014:Quality assurance
 * VERSION::FA:345:30/10/2014:modified comments ratio
 * VERSION::FA:446:20/10/2015:modified constant in iono bent model
 * VERSION::DM:670:25/08/2016: handle NaN with ArithmeticException
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.ionosphere;

import java.io.IOException;
import java.text.ParseException;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.GeodeticPoint;
import fr.cnes.sirius.patrius.forces.atmospheres.solarActivity.SolarActivityDataProvider;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.TopocentricFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.FastMath;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.math.util.Precision;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.time.DateTimeComponents;
import fr.cnes.sirius.patrius.time.TimeScalesFactory;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Bent model for the electronic content used in ionospheric corrections.
 * This class was directly lifted from FORTRAN 90 code.
 * For debugging ease reasons, and for lack of knowlegde on the original code,
 * the ported code is as close as possible to the original, which means it's rather
 * unreadable as it is.
 * 
 * @concurrency thread-safe (require a <cite>synchronized</cite> tag on the
 *              {@link BentModel#computeElectronicCont(AbsoluteDate, Vector3D, Frame)
 *              computeElectronicCont(AbsoluteDate, Vector3D, Frame)} method)
 * 
 * @see IonosphericCorrection
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.3
 * 
 */
public class BentModel implements IonosphericCorrection {

    /** Q3T5 */
    private static final double Q3T5 = 3.e5;
    /** 1 */
    private static final double Q1 = 1.;
    /** 37 */
    private static final double Q37 = 37.;
    /** 12 */
    private static final int Q12 = 12;
    /** 30 */
    private static final int Q30 = 30;
    /** 76 */
    private static final int Q76 = 76;
    /** 80 */
    private static final int Q80 = 80;
    /** 1000 */
    private static final double Q_728 = .728;
    /** 1000 */
    private static final double Q63_75 = 63.75;
    /** 1000 */
    private static final double Q8_9EM4 = 8.9e-4;
    /** 1000 */
    private static final double Q1000 = 1000.;
    /** R */
    private static final double R = 6371.2e3;
    /** SPLAT */
    private static final double SPLAT = 0.9799246;
    /** CPLAT */
    private static final double CPLAT = 0.1993684;
    /** PLON */
    private static final double PLON = 5.078908;
    /** DG */
    private static final double[] DG = { 1.02974426, 0.48869219, -0.57595865 };
    /** H1 */
    private static final double H1 = 1346.92;
    /** H2 */
    private static final double H2 = 526.4;
    /** H3 */
    private static final double H3 = 59.825;
    /** PER */
    private static final double PER = 0.00133;
    /** CENT */
    private static final double[] CENT = { 1.035, 0.957, 0.9 };
    /** 0.05 */
    private static final double QP05 = 0.05;
    /** qp1333 */
    private static final double QP1333 = 0.133333;
    /** qp95 */
    private static final double QP95 = 0.95;
    /** q2p5 */
    private static final double Q2P5 = 2.5;
    /** q10p5 */
    private static final double Q10P5 = 10.5;
    /** q8o15 */
    private static final double Q8O15 = .533333333;
    /** q89p9 */
    private static final double Q89P9 = 1.56905099;
    /** 0.05 */
    private static final double D5 = .0872664625;
    /** d7p5 */
    private static final double D7P5 = .13089969375;
    /** d8 */
    private static final double D8 = .13962634;
    /** d10 */
    private static final double D10 = .174532925;
    /** d16 */
    private static final double D16 = .27925268;
    /** d30 */
    private static final double D30 = .523598775;
    /** d135 */
    private static final double D135 = 2.35619449;
    /** d180 */
    private static final double D180 = 3.1415926536;
    /** pih */
    private static final double PIH = 1.570796268;
    /** pi2 */
    private static final double PI2 = 6.2831853072;
    /** deg */
    private static final double[] DEG = { 1.3089969375, .7853981625, .2617993875 };
    /** so1 */
    private static final double SO1 = .4091749893;
    /** so2 */
    private static final double SO2 = .0172142063;
    /** rn4 */
    private static final double RN4 = .9375;
    /** 1012000. */
    private static final double H1012 = 1012000.;
    
    /** 7 */
    private static final int SEVEN = 7;
    /** 8 */
    private static final int EIGHT = 8;
    /** 9 */
    private static final int NINE = 9;
    /** 24. */
    private static final double TWENTYFOUR = 24.;

    /** CEPT */
    private static final double[] CEPT00 = { 12.2e-6, 8.73e-6, 15.45e-6, 15.45e-6 };
    /** CEPT */
    private static final double[] CEPT01 = { 8.88e-6, 10.38e-6, 9.0e-6, 10.94e-6 };
    /** CEPT */
    private static final double[] CEPT02 = { 2.3e-6, 9.58e-6, 11.9e-6, 12.84e-6 };
    /** CEPT */
    private static final double[] CEPT10 = { 7.62e-6, 4.67e-6, 5.86e-6, 5.86e-6 };
    /** CEPT */
    private static final double[] CEPT11 = { 1.1e-6, 3.34e-6, 4.42e-6, 3.98e-6 };
    /** CEPT */
    private static final double[] CEPT12 = { 0.8e-6, 3.16e-6, 4.41e-6, 4.54e-6 };
    /** CEPT */
    private static final double[] CEPT20 = { 3.92e-6, 3.58e-6, 4.06e-6, 4.06e-6 };
    /** CEPT */
    private static final double[] CEPT21 = { .65e-6, 1.52e-6, 1.44e-6, 1.95e-6 };
    /** CEPT */
    private static final double[] CEPT22 = { 0.5e-6, 2.74e-6, 1.22e-6, 1.55e-6 };
    /** CEPT */
    private static final double[][] CEPT0 = { CEPT00, CEPT01, CEPT02 };
    /** CEPT */
    private static final double[][] CEPT1 = { CEPT10, CEPT11, CEPT12 };
    /** CEPT */
    private static final double[][] CEPT2 = { CEPT20, CEPT21, CEPT22 };
    /** CEPT */
    private static final double[][][] CEPT = { CEPT0, CEPT1, CEPT2 };

    /** SLOP */
    private static final double[] SLOP00 = { -7.5e-8, -3.6e-8, -9.e-8, -9.e-8 };
    /** SLOP */
    private static final double[] SLOP01 = { -3.1e-8, -3.6e-8, -1.e-8, -1.8e-8 };
    /** SLOP */
    private static final double[] SLOP02 = { 4.5e-8, -1.6e-8, -3.5e-8, -2.8e-8 };
    /** SLOP */
    private static final double[] SLOP10 = { -3.4e-8, -.4e-8, -1.2e-8, -1.2e-8 };
    /** SLOP */
    private static final double[] SLOP11 = { 3.e-8, 1.2e-8, .6e-8, 1.4e-8 };
    /** SLOP */
    private static final double[] SLOP12 = { 2.5e-8, 1.3e-8, 1.3e-8, 1.7e-8 };
    /** SLOP */
    private static final double[] SLOP20 = { -.9e-8, -.6e-8, -.7e-8, -.7e-8 };
    /** SLOP */
    private static final double[] SLOP21 = { 1.5e-8, 1.1e-8, 1.7e-8, 1.e-8 };
    /** SLOP */
    private static final double[] SLOP22 = { 1.e-8, .8e-8, 1.6e-8, 1.5e-8 };
    /** SLOP */
    private static final double[][] SLOP0 = { SLOP00, SLOP01, SLOP02 };
    /** SLOP */
    private static final double[][] SLOP1 = { SLOP10, SLOP11, SLOP12 };
    /** SLOP */
    private static final double[][] SLOP2 = { SLOP20, SLOP21, SLOP22 };
    /** SLOP */
    private static final double[][][] SLOP = { SLOP0, SLOP1, SLOP2 };

    /** RATK */
    private static final double[] RATK00 = { .82, .95, 1.07, 1.14 };
    /** RATK */
    private static final double[] RATK01 = { .86, .85, .9, 1.05 };
    /** RATK */
    private static final double[] RATK02 = { .88, .975, 1.05, 1.125 };
    /** RATK */
    private static final double[] RATK03 = { .94, 1.1, 1.115, 1.115 };
    /** RATK */
    private static final double[] RATK10 = { .95, .97, .96, 1.04 };
    /** RATK */
    private static final double[] RATK11 = { .94, .985, .975, 1.005 };
    /** RATK */
    private static final double[] RATK12 = { 1.125, 1.11, 1.085, 1.065 };
    /** RATK */
    private static final double[] RATK13 = { .99, 1.175, 1.08, .94 };
    /** RATK */
    private static final double[] RATK20 = { .985, .9, .86, .995 };
    /** RATK */
    private static final double[] RATK21 = { .925, 1.055, .97, .94 };
    /** RATK */
    private static final double[] RATK22 = { 1.025, 1.09, 1.055, .93 };
    /** RATK */
    private static final double[] RATK23 = { .945, .885, .83, .84 };
    /** RATK */
    private static final double[][] RATK0 = { RATK00, RATK01, RATK02, RATK03 };
    /** RATK */
    private static final double[][] RATK1 = { RATK10, RATK11, RATK12, RATK13 };
    /** RATK */
    private static final double[][] RATK2 = { RATK20, RATK21, RATK22, RATK23 };
    /** RATK */
    private static final double[][][] RATK = { RATK0, RATK1, RATK2 };

    /** YMTAB */
    private static final double[] YMTAB0 =
    { 87.7, 93., 97.8, 102., 102.3, 99.4, 95.1, 91.3, 88., 86.8, 86., 85.2 };
    /** YMTAB */
    private static final double[] YMTAB1 =
    { 96.2, 98., 103.8, 109.5, 112.5, 112.5, 107.5, 101.2, 96.2, 95.4, 97., 98.1 };
    /** YMTAB */
    private static final double[] YMTAB2 =
    { 107.6, 117.7, 140.1, 150.4, 153.3, 154., 150., 140.2, 127.1, 115.5, 109.2, 106.5 };
    /** YMTAB */
    private static final double[] YMTAB3 =
    { 114.4, 125.5, 144.2, 162.7, 175.6, 180.6, 174.8, 157.5, 134.7, 115., 110.1, 110. };
    /** YMTAB */
    private static final double[] YMTAB4 =
    { 113.3, 120.7, 134.9, 158.2, 181.6, 190.5, 177., 152.5, 123., 113.4, 111.5, 110.3 };
    /** YMTAB */
    private static final double[] YMTAB5 =
    { 113.5, 125.4, 139., 158.6, 199.5, 188.3, 188.3, 166.8, 136.9, 119.9, 111.9, 108. };
    /** YMTAB */
    private static final double[] YMTAB6 =
    { 114., 118.2, 125.6, 157., 211.4, 232.3, 211.2, 188.3, 142.5, 124.8, 116.8, 112.5 };
    /** YMTAB */
    private static final double[] YMTAB7 =
    { 122.7, 132., 143.3, 158.3, 187.1, 214.4, 196.8, 185.5, 152.5, 130., 120.7, 117.8 };
    /** YMTAB */
    private static final double[] YMTAB8 =
    { 140.8, 147.5, 155., 167.8, 200., 195.6, 187., 168.3, 144.4, 138.7, 137.7, 137.5 };
    /** YMTAB */
    private static final double[][] YMTAB =
    { YMTAB0, YMTAB1, YMTAB2, YMTAB3, YMTAB4, YMTAB5, YMTAB6, YMTAB7, YMTAB8 };

    /** YRAT */
    private static final double[] YRAT0 = { 1.25, 1.12, 1.04, .95, .92, .92, .92 };
    /** YRAT */
    private static final double[] YRAT1 = { 1.1, 1.06, .99, .88, .78, .73, .7 };
    /** YRAT */
    private static final double[] YRAT2 = { 1.3, 1.21, 1.02, .88, .81, .78, .78 };
    /** YRAT */
    private static final double[] YRAT3 = { 1.09, 1.04, 1.01, .98, .98, .99, 1. };
    /** YRAT */
    private static final double[] YRAT4 = { .95, .96, .97, 1., 1.04, 1.09, 1.13 };
    /** YRAT */
    private static final double[] YRAT5 = { 1.24, 1.24, 1.24, 1.24, 1.33, 1.53, 1.64 };
    /** YRAT */
    private static final double[][] YRAT = { YRAT0, YRAT1, YRAT2, YRAT3, YRAT4, YRAT5 };

    /** CT */
    private static final double[] CT0 =
    { 0., 0., .33333333, .26666667, .25714286, .25396825, .25252525 };
    /** CT */
    private static final double[] CT1 =
    { 0., 0., 0., .2, .22857142, .23809523, .24242424 };
    /** CT */
    private static final double[] CT2 =
    { 0., 0., 0., 0., .14285714, .19047619, .21212121 };
    /** CT */
    private static final double[] CT3 =
    { 0., 0., 0., 0., 0., .11111111, .16161616 };
    /** CT */
    private static final double[] CT4 =
    { 0., 0., 0., 0., 0., 0., .09090909 };
    /** CT */
    private static final double[] CT5 =
    { 0., 0., 0., 0., 0., 0., 0. };
    /** CT */
    private static final double[] CT6 =
    { 0., 0., 0., 0., 0., 0., 0. };
    /** CT */
    private static final double[][] CT =
    { CT0, CT1, CT2, CT3, CT4, CT5, CT6 };

    // coefficients for the year 1960 (JENSEN & CAIN)
    /** G */
    private static final double[] G0 =
    { 0., .304112, .024035, -.031518, -.041794, .016256, -.019523 };
    /** G */
    private static final double[] G1 =
    { 0., .021474, -.051253, .062130, -.045298, -.034407, -.004853 };
    /** G */
    private static final double[] G2 =
    { 0., 0., -.013381, -.024898, -.021795, -0.019447, .003212 };
    /** G */
    private static final double[] G3 =
    { 0., 0., 0., -.006496, .007008, -.000608, .021413 };
    /** G */
    private static final double[] G4 =
    { 0., 0., 0., 0., -.002044, .002775, .001051 };
    /** G */
    private static final double[] G5 =
    { 0., 0., 0., 0., 0., .000697, .000227 };
    /** G */
    private static final double[] G6 =
    { 0., 0., 0., 0., 0., 0., .001115 };
    /** G */
    private static final double[][] G =
    { G0, G1, G2, G3, G4, G5, G6 };
    /** H */
    private static final double[] HR0 =
    { 0., 0., 0., 0., 0., 0., 0. };
    /** H */
    private static final double[] HR1 =
    { 0., -.057989, .033124, .014870, -.011825, -.000796, -.005758 };
    /** H */
    private static final double[] HR2 =
    { 0., 0., -.001579, -.004075, .010006, -.002, -.008735 };
    /** H */
    private static final double[] HR3 =
    { 0., 0., 0., .00021, .00043, .004597, -.003406 };
    /** H */
    private static final double[] HR4 =
    { 0., 0., 0., 0., .001385, .002421, -.000118 };
    /** H */
    private static final double[] HR5 =
    { 0., 0., 0., 0., 0., -.001218, -.001116 };
    /** H */
    private static final double[] HR6 =
    { 0., 0., 0., 0., 0., 0., -.000325 };
    /** H */
    private static final double[][] H =
    { HR0, HR1, HR2, HR3, HR4, HR5, HR6 };

    /** flux max */
    private static final double FLUXMAX = 130.;

    /** Constant for the deltaT computation: */
    private static final double CDELTAT = 40.3;

    /** flux */
    private double flux;
    /** dflux */
    private double dflux;
    /** station latitude */
    private double flat;
    /** station longitude */
    private double flon;
    /** olat */
    private double olat;
    /** olon */
    private double olon;
    /** hlat */
    private double hlat;
    /** FOF2 */
    private double fof2;
    /** iuf data */
    private int[] iuf;
    /** ium data */
    private double[][] uf;
    /** uf data */
    private int[] ium;
    /** um data */
    private double[][] um;
    /** p (PROFL3, MAGFIN) */
    private double[] pos;

    /** station elevation */
    private double elev;
    /** station azimut */
    private double azim;
    /** month */
    private double mon;
    /** iDay */
    private double iDay;
    /** time */
    private double time;

    /** HS : satellite altitude */
    private double hs;
    /** DIST : satellite - station distance */
    private double dist;
    /** HM */
    private double hm;

    /** earth body shape */
    private final BodyShape earthShape;
    /** solar activity provider */
    private final SolarActivityDataProvider solarAct;
    /** usk data provider */
    private final USKProvider uskProv;
    /** solar tasks provider */
    private final R12Provider r12Prov;
    /** measured signal frequency */
    private final double frequency;
    /** coordinates of the position of the STATION in the frameSta frame */
    private final Vector3D station;
    /** frame in witch the STATION coordinates are expressed */
    private final Frame frameSta;

    /**
     * Constructor for the Bent ionospheric correction model.
     * 
     * @param r12Provider
     *        provider for the R12 value
     * @param solarActivity
     *        provider for the solar activity
     * @param uskProvider
     *        provider for the model data
     * @param earth
     *        ther earth body shape
     * @param inStation
     *        coordinates of the position of the STATION in the frameSta frame
     * @param inFrameSta
     *        frame in witch the STATION coordinates are expressed
     * @param freq
     *        measured signal frequency [Hz]
     */
    public BentModel(final R12Provider r12Provider,
        final SolarActivityDataProvider solarActivity, final USKProvider uskProvider,
        final BodyShape earth, final Vector3D inStation, final Frame inFrameSta, final double freq) {

        // initialisations
        this.earthShape = earth;
        this.solarAct = solarActivity;
        this.uskProv = uskProvider;
        this.r12Prov = r12Provider;
        this.frequency = freq;
        this.station = inStation;
        this.frameSta = inFrameSta;
    }

    /** {@inheritDoc} */
    @Override
    public double computeSignalDelay(final AbsoluteDate date,
            final Vector3D satellite,
                                     final Frame frameSat) throws PatriusException, IOException, ParseException {
        // Electronic content computation:
        final double tec = this.computeElectronicCont(date, satellite, frameSat);
        return CDELTAT / (Constants.SPEED_OF_LIGHT * this.frequency * this.frequency) * tec;
    }

    /**
     * Computation of the electric content between the station and the satellite at a date.
     * <p>
     * <i>Note: This method needs a <cite>synchronized</cite> tag to make this computation thread
     * safe (not thread safe otherwise).</i>
     * </p>
     * 
     * @param date
     *        current date
     * @param satellite
     *        coordinates of the position of the SATELLITE in the frameSta frame
     * @param frameSat
     *        frame in witch the SATELLITE coordinates are expressed
     * @throws IOException
     *         if an error occur with the inputs
     * @throws ParseException
     *         if an error occur with the parsing of files
     * @throws PatriusException
     *         if an error occur with the dates and frames management
     * @return the electronic content between the station and the satellite at this date.
     */
    public synchronized double computeElectronicCont(final AbsoluteDate date,
            final Vector3D satellite, final Frame frameSat) throws PatriusException, IOException,
            ParseException {

        // station topocentric frame
        final GeodeticPoint stationGeoPoint = this.earthShape.transform(this.station, this.frameSta, date);
        final TopocentricFrame stationTopoFrame = new TopocentricFrame(this.earthShape, stationGeoPoint, "earth");
        this.flat = stationGeoPoint.getLatitude();
        this.flon = stationGeoPoint.getLongitude();
        // normalisation on [0;2PI] as in the original algorithm
        this.flon = MathUtils.normalizeAngle(this.flon, FastMath.PI);

        // satellite position in the station's topocentric frame
        final Transform t = frameSat.getTransformTo(stationTopoFrame, date);
        final Vector3D satInTopoFrame = t.transformPosition(satellite);
        // dist in km
        this.dist = satInTopoFrame.getNorm() / Q1000;

        // satellite azimut and elevation computation in the station's topocentric frame
        this.elev = stationTopoFrame.getElevation(satInTopoFrame, stationTopoFrame, date);
        this.azim = stationTopoFrame.getAzimuth(satInTopoFrame, stationTopoFrame, date);

        // date
        final DateTimeComponents components = date.getComponents(TimeScalesFactory.getTT());
        this.mon = components.getDate().getMonth();
        this.iDay = components.getDate().getDay();

        // r12
        final double r12 = this.r12Prov.getR12(date);

        // USK data from the model
        final USKData uskData = this.uskProv.getData(date, r12);
        this.ium = uskData.getIum();
        this.iuf = uskData.getIuf();
        this.uf = uskData.getUf();
        this.um = uskData.getUm();

        // s12 computation
        final double s12 = Q63_75 + Q_728 * r12 + Q8_9EM4 * r12 * r12;

        // time
        final double rat = 6371.2;
        // 15 degrees in radians --> radians per hour
        final double hr = 0.2617993875;
        // time is in radians
        final double tu = (components.getTime().getSecondsInDay() / (60. * 60.));
        this.time = tu * hr;

        // satellite altitude HS computation
        final double rsat = MathLib.sqrt(rat * rat + this.dist * this.dist
            + 2.0 * rat * this.dist * MathLib.sin(this.elev));
        this.hs = (rsat - rat) * Q1000;

        // flux
        this.flux = this.solarAct.getInstantFluxValue(date);
        if (this.flux <= 0.) {
            this.flux = s12;
        }
        this.flux = MathLib.min(FLUXMAX, this.flux);

        this.dflux = this.flux - s12;

        // call to the first sub method PROFL3 from the associated subroutine.
        this.profl3();

        // call to the first sub method PROFL2 from the associated subroutine.
        // the return is the electronic content
        return this.profl2();
    }

    /**
     * Implementation of the PROFL3 subroutine.
     */
    private void profl3() {

        // containers init
        this.pos = new double[3];
        final double[] c = new double[3];
        double[] com;

        // cosine and sine values
        final double[] sincosflat = MathLib.sinAndCos(this.flat);
        final double[] sincoselev = MathLib.sinAndCos(this.elev);
        final double[] sincosazim = MathLib.sinAndCos(this.azim);
        final double clat = sincosflat[1];
        final double slat = sincosflat[0];
        final double cel = sincoselev[1];
        final double sel = sincoselev[0];
        final double caz = sincosazim[1];
        final double saz = sincosazim[0];

        this.pos[2] = Q3T5;
        final double te = this.time - FastMath.PI;

        // control variable
        boolean endLoop = false;

        // control loop
        while (!endLoop) {

            // SUB-IONOSPHERIC POINT
            final double sf = MathLib.divide(R * cel, R + this.pos[2]);
            final double cf = MathLib.sqrt(MathLib.max(0.0, Q1 - sf * sf));
            final double sa = cel * cf - sel * sf;
            final double ca = sel * cf + cel * sf;
            final double snlat = slat * ca + clat * sa * caz;
            final double cnlat = MathLib.sqrt(MathLib.max(0.0, Q1 - snlat * snlat));
            this.olat = MathLib.atan(MathLib.divide(snlat, cnlat));
            final double sdlon = MathLib.divide(saz * sa, cnlat);
            final double cdlon = MathLib.sqrt(MathLib.max(0.0, Q1 - sdlon * sdlon));
            this.olon = this.flon + MathLib.atan(MathLib.divide(sdlon, cdlon));

            // magnetic latitude computation
            final double sml = snlat * SPLAT + cnlat * CPLAT * MathLib.cos(this.olon - PLON);
            final double cml = MathLib.sqrt(MathLib.max(0.0, Q1 - sml * sml));
            this.hlat = MathLib.atan(MathLib.divide(sml, cml));

            // hmf2 computation
            this.pos[0] = this.olat;
            this.pos[1] = this.olon;

            // call to the magfin subroutine, that modifies pos and computes com
            com = this.magfin();

            final double tmp = com[1] * com[1] + com[2] * com[2];
            final double tmp2 = MathLib.divide(MathLib.atan(MathLib.divide(-com[0], MathLib.sqrt(tmp))),
                MathLib.sqrt(cnlat));
            c[0] = MathLib.atan(tmp2);
            c[1] = this.pos[1];
            c[2] = this.pos[0];

            // call to the first sub method USKF from the associated subroutine.
            final double h3000 = this.uskf(this.ium, this.um, c, te);

            this.hm = (H1 - H2 * h3000 + H3 * h3000 * h3000) * Q1000;
            if (MathLib.abs(this.pos[2] - this.hm) < Q1000) {
                endLoop = true;
            } else {
                this.pos[2] = this.hm;
            }
        }

        // computation of fof2 and daily flux adjustment

        // call to the first sub method USKF from the associated subroutine.
        this.fof2 = this.uskf(this.iuf, this.uf, c, te);

        int lat1 = 0;
        int lat2 = 0;
        if (this.hlat < DG[lat2]) {
            lat2 = 1;
            if (this.hlat <= DG[lat2]) {
                lat1 = 1;
                if (MathLib.abs(this.hlat - DG[lat2]) > Precision.DOUBLE_COMPARISON_EPSILON) {
                    lat2 = 2;
                    if (this.hlat <= DG[lat2]) {
                        lat1 = 2;
                    }
                }
            }
        }

        // Finalisation
        double cnt = CENT[lat1];
        if (lat1 != lat2) {
            cnt = cnt + MathLib.divide((CENT[lat2] - CENT[lat1]) * (DG[lat1] - this.hlat),
                (DG[lat1] - DG[lat2]));
        }

        this.fof2 = this.fof2 * (PER * this.dflux + cnt);
    }

    /**
     * Internal computations for the PROFL3 method.
     * 
     * @param kd
     *        kd
     * @param d
     *        d
     * @param c
     *        c
     * @param te
     *        te
     * @return om
     */
    private double uskf(final int[] kd,
                        final double[][] d, final double[] c, final double te) {

        // initializations
        final double k0 = kd[0];
        final double[] gG = new double[Q76];
        final double[] cotlo = new double[6];
        final double[] sitlo = new double[6];
        final double[] as = new double[SEVEN];
        final double[] bs = new double[SEVEN];
        int j = 0;

        int nff = kd[EIGHT] + 1;

        // geographic functions
        gG[0] = 1.0;
        gG[1] = MathLib.sin(c[0]);
        final double sx = gG[1];

        for (int i = 1; i < k0; i++) {
            gG[i + 1] = sx * gG[i];
        }

        // initialize difference
        double kdif = kd[1] - k0;
        final double cx1 = MathLib.cos(c[2]);
        double cx = cx1;
        double t = c[1];

        boolean endLoop = false;
        // stop when j = 7
        while (!endLoop) {
            final int kc = kd[j] + 4;
            final double[] sincost = MathLib.sinAndCos(t);
            gG[kc - 3] = cx * sincost[1];
            gG[kc - 2] = cx * sincost[0];

            if (kdif != 2) {
                final int kn = kd[j + 1];

                for (int i = kc - 1; i < kn; i = i + 2) {
                    gG[i] = sx * gG[i - 2];
                    gG[i + 1] = sx * gG[i - 1];
                }
            }

            if (j == SEVEN) {
                // last index reached
                endLoop = true;
            } else {
                kdif = kd[j + 2] - kd[j + 1];
                if (MathLib.abs(kdif) < Precision.DOUBLE_COMPARISON_EPSILON) {
                    // precision reached
                    endLoop = true;
                } else {
                    cx = cx * cx1;
                    j++;
                    t = c[1] * (j + 1);
                }
            }
        }

        // EX-S/P SICOJT
        cotlo[0] = MathLib.cos(te);
        sitlo[0] = MathLib.sin(te);

        // Complete array
        for (int i = 2; i < SEVEN; i++) {
            cotlo[i - 1] = cotlo[0] * cotlo[i - 2] - sitlo[0] * sitlo[i - 2];
            sitlo[i - 1] = cotlo[0] * sitlo[i - 2] + sitlo[0] * cotlo[i - 2];
        }

        // EX-S/P AJBJ
        int m = kd[NINE] + 1;
        nff = kd[EIGHT] + 1;

        // *** call to this method for cyclomatic complexity issues ***
        // Computes the as array for the usk method.
        this.completeAs(as, m, d, nff, gG);
        // Computes the bs array for the usk method
        this.completeBs(bs, m, d, nff, gG);

        // EX-S/P ABSICO
        double ome = as[0];
        m = kd[NINE];

        // compute final result
        for (int i = 1; i <= m; i++) {
            ome = ome + as[i] * cotlo[i - 1] + bs[i] * sitlo[i - 1];
        }

        // return final result
        return ome;
    }

    /**
     * Computes the as array for the usk method. Called for cyclomatic complexity issues.
     * 
     * @param initAs
     *        previous as
     * @param m
     *        m
     * @param d
     *        d
     * @param nff
     *        nff
     * @param gG
     *        g
     */
    private void completeAs(final double[] initAs, final int m, final double[][] d,
                            final int nff, final double[] gG) {

        final double[] as = initAs;

        for (int i = 1; i <= m; i++) {
            as[i - 1] = 0.;

            for (int k = 0; k < nff; k++) {
                as[i - 1] = as[i - 1] + d[2 * i - 2][k] * gG[k];
            }
        }
    }

    /**
     * Computes the bs array for the usk method. Called for cyclomatic complexity issues.
     * 
     * @param initBs
     *        previous bs
     * @param m
     *        m
     * @param d
     *        d
     * @param nff
     *        nff
     * @param gG
     *        g
     */
    private void completeBs(final double[] initBs, final int m, final double[][] d,
                            final int nff, final double[] gG) {

        final double[] bs = initBs;

        for (int i = 2; i <= m; i++) {
            bs[i - 1] = 0.;

            for (int k = 0; k < nff; k++) {
                bs[i - 1] = bs[i - 1] + d[2 * i - 3][k] * gG[k];
            }
        }
    }

    /**
     * Implementation of the PROFL2 subroutine.
     * 
     * @return the electric content.
     */
    // CHECKSTYLE: stop MethodLength check
    private double profl2() {
        // CHECKSTYLE: resume MethodLength check

        final double[] xk = new double[3];

        this.hlat = MathLib.abs(this.hlat);
        double tloc = this.time + this.olon + PI2;

        // The proper line should be :
        // tloc = MathUtils.normalizeAngle(tloc, FastMath.PI);
        // but since the original algorithm relies on a modulus instead,
        // we keep it similar :
        tloc = tloc % PI2;

        // Computation of YM : half thickness of the profile's lower part

        double t12 = tloc / D30;
        double t1 = MathLib.floor(t12);
        int lt1 = (int) t1;
        int lt2 = lt1 + 1;
        if (lt1 == Q12) {
            lt2 = lt1;
        }
        if (lt1 < 1) {
            lt1 = Q12;
        }
        t1 = t12 - t1;
        int if1 = (int) (this.fof2 - QP95);
        int if2 = (int) (this.fof2 - QP05);
        if1 = this.correctIF(if1);
        if2 = this.correctIF(if2);

        double ym = (YMTAB[if1 - 1][lt1 - 1] + (YMTAB[if1 - 1][lt2 - 1]
            - YMTAB[if1 - 1][lt1 - 1]) * t1) * Q1000;

        ym = this.computeYM(ym, if1, if2, t1, lt1, lt2);

        // Computation of dsza : difference bewteen the daily value and the
        // mean value over a year of the zenithal solar angle at midday
        final double day = (this.mon - 1) * Q30 + this.iDay - Q80;
        double dsza = SO1 * MathLib.sin(SO2 * day);
        dsza = this.computeDSZA(dsza);

        // seasons' effects on YM
        double s12 = 4. - dsza / D8;
        if1 = (int) s12;
        double s1 = if1;
        s1 = s12 - s1;
        if2 = if1 + 1;
        // computation of ym
        ym = this.computeYM(t12, tloc, lt1, lt2, t1, s1, if1, if2, ym);

        // Computation of the XK(I) constants on the profile's higher part
        final double fqf2 = RN4 * this.fof2;
        int i1 = 2;
        int i2 = 2;
        final int[] i1i2 = this.checkI1I2(i1, i2);
        i1 = i1i2[0];
        i2 = i1i2[1];

        final double xj = (fqf2 + 1.) / 3.;
        int j = (int) xj;
        final double xf;
        // interpolation using XJ : XJ<=1, we'll use SLOP(1,:,:)
        // XJ>=4, we'll use SLOP(4,:,:)
        // btw 1 & 4, SLOP is interpolated
        // using XJ
        if (j < 1) {
            xf = 0;
            j = 1;
        } else if (j > 3) {
            xf = 1.;
            j = 3;
        } else {
            xf = xj - j;
            // j unchanged (1,2 or 3)
        }
        // call to the method to fill the xk array
        this.fillXK(j, i1, i2, xf, xk);

        // Applies saisons effects of dsza to XK
        t12 = tloc / DEG[2] - (double) EIGHT;
        if (t12 < 0.) {
            t12 = t12 + TWENTYFOUR;
        }
        t12 = t12 / 6. + 1.;
        lt1 = (int) t12;
        t1 = lt1;
        lt2 = lt1 + 1;
        if (lt2 > 4) {
            lt2 = 1;
        }
        s12 = Q2P5 - dsza / D16;
        if1 = (int) s12;
        s1 = if1;
        s1 = s12 - s1;
        if2 = if1 + 1;
        for (int m = 1; m < 4; m++) {
            final double rat1 = RATK[m - 1][lt1 - 1][if1 - 1] + (RATK[m - 1][lt1 - 1][if2 - 1]
                - RATK[m - 1][lt1 - 1][if1 - 1]) * s1;
            final double rat2 = RATK[m - 1][lt2 - 1][if1 - 1] + (RATK[m - 1][lt2 - 1][if2 - 1]
                - RATK[m - 1][lt2 - 1][if1 - 1]) * s1;
            final double rat = rat1 + (rat2 - rat1) * (t12 - t1);
            xk[m - 1] = xk[m - 1] * rat;
        }

        // computation of YT
        // half thickness of the profile's high part
        double conv = 1.;
        if (this.fof2 > Q10P5) {
            conv = QP1333 * (this.fof2 - Q10P5) + 1.;
        }
        final double yt = conv * ym;

        // computation of XNTNM
        // Classical parameter TAU or amounting thickness
        final double xntnm = this.computeXNTNM(ym, xk, yt);

        // computation of the vertical (TOTN) and oblical (TOTNA) electronic content.
        final double totn = xntnm * 1.24e10 * (this.fof2 * this.fof2);
        final double ce = MathLib.cos(this.elev);
        final double rit = (R / (R + this.hm)) * (R / (R + this.hm));
        final double den2 = 1. - rit * (ce * ce);
        final double den = MathLib.sqrt(MathLib.max(0.0, den2));
        return MathLib.divide(totn, den);
    }

    /**
     * DSZA computation for the PROFL2 method.
     * 
     * @param dszaIn
     *        initial dsza
     * @return modified dsza
     */
    private double computeDSZA(final double dszaIn) {
        double dsza = dszaIn;
        if (MathLib.abs(this.olat) < SO1) {
            final double sang = this.olat / SO1;
            final double cang = MathLib.sqrt(1. - MathLib.abs(sang * sang));
            final double dang = MathLib.atan(MathLib.divide(sang, cang));
            final double asza = SO1 * (cang + sang * dang) / PIH;
            dsza = asza - MathLib.abs(this.olat - dszaIn);
        } else if (this.olat < 0.) {
            dsza = -dszaIn;
        }
        return dsza;
    }

    /**
     * YM computation for the PROFL2 method.
     * 
     * @param ymIn
     *        initial YM
     * @param if1
     *        if1
     * @param if2
     *        if2
     * @param t1
     *        t1
     * @param lt1
     *        lt1
     * @param lt2
     *        lt2
     * @return modified YM
     */
    private double computeYM(final double ymIn, final int if1, final int if2, final double t1,
                             final int lt1, final int lt2) {
        double ym = ymIn;
        if (if1 != if2) {
            final double ym2 = (YMTAB[if2 - 1][lt1 - 1] + (YMTAB[if2 - 1][lt2 - 1] - YMTAB[if2 - 1][lt1 - 1]) * t1)
                * Q1000;
            final double f1 = if1;
            ym = ymIn + (ym2 - ymIn) * (this.fof2 - f1 - 1.);
        }
        return ym;
    }

    /**
     * Fills the initial xk array for the PROFL2 method.
     * 
     * @param j
     *        j
     * @param i1
     *        i1
     * @param i2
     *        i2
     * @param xf
     *        xf
     * @param xk
     *        array to be filled
     */
    private void fillXK(final int j, final int i1,
                        final int i2, final double xf, final double[] xk) {

        for (int m = 1; m < 4; m++) {
            double slp = (SLOP[m - 1][i1 - 1][j] - SLOP[m - 1][i1 - 1][j - 1])
                * xf + SLOP[m - 1][i1 - 1][j - 1];
            double cpt = (CEPT[m - 1][i1 - 1][j]
                - CEPT[m - 1][i1 - 1][j - 1]) * xf + CEPT[m - 1][i1 - 1][j - 1];
            if (i1 != i2) {
                final double del = (this.hlat - DEG[i1 - 1]) / (DEG[i2 - 1] - DEG[i1 - 1]);
                slp = slp + ((SLOP[m - 1][i2 - 1][j]
                    - SLOP[m - 1][i2 - 1][j - 1])
                    * xf + SLOP[m - 1][i2 - 1][j - 1] - slp) * del;
                cpt = cpt + ((CEPT[m - 1][i2 - 1][j]
                    - CEPT[m - 1][i2 - 1][j - 1]) * xf + CEPT[m - 1][i2 - 1][j - 1] - cpt) * del;
            }
            xk[m - 1] = slp * this.flux + cpt;
        }
    }

    /**
     * Computes the XNTNM parameter for the PROFL2 method.
     * "Classical parameter TAU or amounting thickness."
     * 
     * @param ym
     *        ym
     * @param xk
     *        xk
     * @param yt
     *        yt
     * @return XNTNM
     */
    private double computeXNTNM(final double ym, final double[] xk,
                                final double yt) {
        // initializations
        final double[] dh = new double[3];
        double xntnm = 0.;
        final double[] hH = new double[4];
        final double d = MathLib.divide(-(1. - MathLib.sqrt(1. + (xk[0] * yt) * (xk[0] * yt))), xk[0]);
        hH[0] = this.hm + d;
        // condition on satellite altitude (HS)
        if (this.hs <= hH[0]) {
            if (this.hs > (this.hm - ym)) {
                // compute distance satellite-station distance
                this.dist = this.hm - this.hs;
                if (this.hs < this.hm) {
                    xntnm = Q8O15 * ym - this.dist
                        + 2. * MathLib.pow(this.dist, 3) / (3. * ym * ym)
                        - MathLib.pow(this.dist, 5) / (5. * MathLib.pow(this.dist, 4));
                } else {
                    xntnm = Q8O15 * ym - this.dist
                        + MathLib.pow(this.dist, 3) / (3. * yt * yt);
                }
            }
        } else {
            // if satellite altitude upper than hH[0]
            final double delh = (H1012 - hH[0]) / 3.;
            hH[1] = hH[0] + delh;
            hH[2] = hH[1] + delh;
            hH[3] = this.hs;
            int m = 3;
            // stop when m = 1
            while ((this.hs <= hH[m - 1]) && (m > 1)) {
                hH[m - 1] = hH[m];
                m = m - 1;
            }
            // m = 1
            while (m > 0) {
                dh[m - 1] = hH[m] - hH[m - 1];
                final double rk = MathLib.divide(1., xk[m - 1]);
                double ex = 0.;
                final double arg = xk[m - 1] * dh[m - 1];
                if (arg < Q37) {
                    ex = MathLib.exp(-arg);
                }
                xntnm = rk + ex * (xntnm - rk);
                m = m - 1;
            }
            final double temp = Q8O15 * ym + d - MathLib.divide(MathLib.pow(d, 3), (3. * yt * yt));
            final double temp1 = 1. - (MathLib.divide(d, yt)) * (MathLib.divide(d, yt));
            xntnm = temp1 * xntnm + temp;
        }
        // return the XNTNM parameter for the PROFL2 method
        return xntnm;
    }

    /**
     * Computation of YM for the PROFL2 method.
     * 
     * @param t12in
     *        t12
     * @param tloc
     *        tloc
     * @param lt1in
     *        lt1
     * @param lt2in
     *        lt2
     * @param t1in
     *        t1
     * @param s1
     *        s1
     * @param if1
     *        if1
     * @param if2
     *        if2
     * @param ymIn
     *        ym
     * @return YM
     */
    private double computeYM(final double t12in, final double tloc, final int lt1in, final int lt2in,
                             final double t1in, final double s1, final int if1, final int if2, final double ymIn) {
        // initialization
        double rat = 0.;
        double t12 = t12in;
        int lt1 = lt1in;
        int lt2 = lt2in;
        double t1 = t1in;
        double ym = ymIn;
        // first condition on hlat
        if (this.hlat > D5) {
            t12 = (tloc + D7P5) / PIH;
            lt1 = (int) t12;
            t1 = lt1;
            lt2 = lt1 + 1;
            // check lt2
            if (lt2 > 4) {
                lt2 = 1;
            }
            // check lt1
            if (lt1 < 1) {
                lt1 = 4;
            }
            // update rat
            final double rat1 = YRAT[lt1 - 1][if1 - 1] + (YRAT[lt1 - 1][if2 - 1]
                - YRAT[lt1 - 1][if1 - 1]) * s1;
            final double rat2 = YRAT[lt2 - 1][if1 - 1] + (YRAT[lt2 - 1][if2 - 1]
                - YRAT[lt2 - 1][if1 - 1]) * s1;
            rat = rat1 + (rat2 - rat1) * (t12 - t1);
        }
        // second condition on hlat
        if (this.hlat < DEG[2]) {
            t12 = (tloc + D135) / D180 - 1.;
            if (t12 > 1.) {
                t12 = 2. - t12;
            }
            if (t12 < 0.) {
                t12 = -t12;
            }
            // update rat
            final double rat1 = YRAT[4][if1 - 1] + (YRAT[4][if2 - 1] - YRAT[4][if1 - 1]) * s1;
            final double rat2 = YRAT[5][if1 - 1] + (YRAT[5][if2 - 1] - YRAT[5][if1 - 1]) * s1;
            final double ratm = rat1 + (rat2 - rat1) * t12;
            rat = ratm + (rat - ratm) * (this.hlat - D5) / D10;
            if (this.hlat < D5) {
                rat = ratm;
            }
        }
        ym = ym * rat;
        // return YM for the PROFL2 method
        return ym;
    }

    /**
     * Correction of the if1 and if2 integer for the PROFL2 method.
     * 
     * @param ifIn
     *        if1 or if2
     * @return corrected if1 or if2.
     */
    private int correctIF(final int ifIn) {
        int ifOut = ifIn;
        if (ifOut < 1) {
            ifOut = 1;
        }
        if (ifOut > NINE) {
            ifOut = NINE;
        }
        return ifOut;
    }

    /**
     * Corrects the i1 and i2 integers for the PROFL2 method.
     * 
     * @param i1in
     *        i1
     * @param i2in
     *        i2
     * @return i1 and i2
     */
    private int[] checkI1I2(final int i1in, final int i2in) {
        // Initialize output i1 and i2
        int i1 = i1in;
        int i2 = i2in;
        // Correct i1 and i2
        if (this.hlat < DEG[1]) {
            i1 = 3;
            if (this.hlat <= DEG[2]) {
                i2 = 3;
            }
        } else if (MathLib.abs(this.hlat - DEG[1]) > Precision.DOUBLE_COMPARISON_EPSILON) {
            i2 = 1;
            if (this.hlat >= DEG[0]) {
                i1 = 1;
            }
        }
        // Return array with corrected integers
        return new int[] { i1, i2 };
    }

    /**
     * Adaptation of the MAGFIN subroutine used in profl3.
     * 
     * @return the com array
     */
    private double[] magfin() {

        // initialisations
        final double[][] p = new double[SEVEN][SEVEN];
        final double[] cp = new double[SEVEN];
        final double[] sp = new double[SEVEN];
        final double[][] dp = new double[SEVEN][SEVEN];
        final double[] aor = new double[SEVEN];

        // first values
        p[0][0] = 1.;
        cp[0] = 1.;
        dp[0][0] = 0.;
        sp[0] = 0.;

        // position
        if (MathLib.abs(this.pos[0]) > Q89P9) {
            this.pos[0] = MathLib.copySign(Q89P9, this.pos[0]);
            this.pos[1] = 0.0;
        }

        // second values
        final double ar = R / (R + this.pos[2]);
        final double[] sincos0 = MathLib.sinAndCos(this.pos[0]);
        final double c = sincos0[0];
        final double s = sincos0[1];
        final double[] sincos1 = MathLib.sinAndCos(this.pos[1]);
        sp[1] = sincos1[0];
        cp[1] = sincos1[1];
        aor[0] = ar * ar;
        aor[1] = aor[0] * ar;

        // recurrence
        for (int m = 2; m < SEVEN; m++) {
            sp[m] = sp[1] * cp[m - 1] + cp[1] * sp[m - 1];
            cp[m] = cp[1] * cp[m - 1] - sp[1] * sp[m - 1];
            aor[m] = ar * aor[m - 1];
        }

        // initialize results
        double bv = 0.;
        double bn = 0.;
        double bphi = 0.;

        // recurrence
        for (int n = 2; n < EIGHT; n++) {
            // Initialize variables for second recurrence
            final double fn = n;
            double sumr = 0.;
            double sumt = 0.;
            double sump = 0.;

            // recurrence
            for (int m = 1; m <= n; m++) {

                if (m == n) {
                    // last step
                    p[n - 1][n - 1] = s * p[n - 2][n - 2];
                    dp[n - 1][n - 1] = s * dp[n - 2][n - 2] + c * p[n - 2][n - 2];
                } else if (m == n - 1) {
                    // step before last
                    p[n - 1][m - 1] = c * p[n - 2][m - 1];
                    dp[n - 1][m - 1] = c * dp[n - 2][m - 1] - s * p[n - 2][m - 1];
                } else {
                    // any step but the last two
                    p[n - 1][m - 1] = c * p[n - 2][m - 1] - CT[m - 1][n - 1] * p[n - 3][m - 1];
                    dp[n - 1][m - 1] = c * dp[n - 2][m - 1] - s * p[n - 2][m - 1]
                        - CT[m - 1][n - 1] * dp[n - 3][m - 1];
                }
                final double fm = m - 1;
                final double ts = G[m - 1][n - 1] * cp[m - 1] + H[m - 1][n - 1] * sp[m - 1];
                sumr = sumr + p[n - 1][m - 1] * ts;
                sumt = sumt + dp[n - 1][m - 1] * ts;
                sump = sump + fm * p[n - 1][m - 1] * (-G[m - 1][n - 1] * sp[m - 1]
                    + H[m - 1][n - 1] * cp[m - 1]);
            }

            // results
            bv = bv + aor[n - 1] * fn * sumr;
            bn = bn - aor[n - 1] * sumt;
            bphi = bphi - aor[n - 1] * sump;
        }

        // (1)=Z : upper vertical (2)=X : north - (3)=Y : est
        final double[] une = new double[3];
        une[0] = -bv;
        une[1] = bn;
        une[2] = MathLib.divide(-bphi, s);

        // final results
        return une;
    }
}
