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
 * HISTORY
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3282:22/05/2023:[PATRIUS] Amelioration gestion attractions gravitationnelles
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.5:FA:FA-2326:27/05/2020:Orbits - Correction equals & HashCode 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.4:DM:DM-2135:04/10/2019:[PATRIUS] Methodes equals() et hashCode() non implementees dans la classe TLE
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS/COLOSUS] Mise en conformite code avec nouveau standard codage DYNVOL
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:426:30/10/2015: allow orbit instantiation in non inertial frame
 * VERSION::FA:829:25/01/2017:Protection of trigonometric methods
 * VERSION::FA:1281:30/08/2017: Javadoc correction
 * VERSION::DM:1798:10/12/2018: Add getN() after AlternateEquinoctialParameters creation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.orbits;

import java.util.Collection;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.math.analysis.interpolation.HermiteInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.math.util.MathUtils;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.CartesianParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.IOrbitalParameters;
import fr.cnes.sirius.patrius.orbits.orbitalparameters.KeplerianParameters;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinates;
import fr.cnes.sirius.patrius.time.AbsoluteDate;

/**
 * This class handles traditional keplerian orbital parameters.
 * <p>
 * The parameters used internally are the keplerian elements (see {@link KeplerianParameters} for more information.
 * </p>
 * <p>
 * The instance <code>KeplerianOrbit</code> is guaranteed to be immutable.
 * </p>
 * 
 * @see Orbit
 * @see CircularOrbit
 * @see CartesianOrbit
 * @see EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public final class KeplerianOrbit extends Orbit {

    /** Serializable UID. */
    private static final long serialVersionUID = 7593919633854535287L;
    
    /** Root int for hash code. */
    private static final int ROOTINT = 351;

    /** Orbital parameters. */
    private final KeplerianParameters parameters;

    /**
     * Creates a new instance.
     * 
     * @param parametersIn
     *        orbital parameters
     * @param frame
     *        the frame in which the parameters are defined
     * @param date
     *        date of the orbital parameters
     */
    public KeplerianOrbit(final IOrbitalParameters parametersIn, final Frame frame,
        final AbsoluteDate date) {
        super(frame, date, parametersIn.getMu());
        this.parameters = parametersIn.getKeplerianParameters();
    }

    /**
     * Creates a new instance.
     * 
     * @param a
     *        semi-major axis (m), negative for hyperbolic orbits
     * @param e
     *        eccentricity (e >= 0)
     * @param i
     *        inclination (rad)
     * @param pa
     *        perigee argument (&omega;, rad)
     * @param raan
     *        right ascension of ascending node (&Omega;, rad)
     * @param anomaly
     *        mean, eccentric or true anomaly (rad)
     * @param type
     *        type of anomaly
     * @param frame
     *        the frame in which the parameters are defined
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     * @exception IllegalArgumentException
     *            if a and e don't match for hyperbolic orbits,
     *            or v is out of range for hyperbolic orbits
     */
    public KeplerianOrbit(final double a, final double e, final double i, final double pa,
        final double raan, final double anomaly, final PositionAngle type, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(frame, date, mu);
        this.parameters = new KeplerianParameters(a, e, i, pa, raan, anomaly, type, mu);
    }

    /**
     * Constructor from cartesian parameters.
     * 
     * @param pvCoordinates
     *        the PVCoordinates of the satellite
     * @param frame
     *        the frame in which are defined the {@link PVCoordinates}
     * @param date
     *        date of the orbital parameters
     * @param mu
     *        central attraction coefficient (m<sup>3</sup>/s<sup>2</sup>)
     */
    public KeplerianOrbit(final PVCoordinates pvCoordinates, final Frame frame,
        final AbsoluteDate date, final double mu) {
        super(pvCoordinates, frame, date, mu);
        this.parameters = new CartesianParameters(pvCoordinates, mu).getKeplerianParameters();
    }

    /**
     * Constructor from any kind of orbital parameters.
     * 
     * @param op
     *        orbital parameters to copy
     */
    public KeplerianOrbit(final Orbit op) {
        super(op.getPVCoordinates(), op.getFrame(), op.getDate(), op.getMu());
        this.parameters = op.getParameters().getKeplerianParameters();
    }

    /** {@inheritDoc} */
    @Override
    public IOrbitalParameters getParameters() {
        return this.parameters;
    }

    /**
     * Getter for underlying keplerian parameters.
     * 
     * @return keplerian parameters
     */
    public KeplerianParameters getKeplerianParameters() {
        return this.parameters;
    }

    /** {@inheritDoc} */
    @Override
    public OrbitType getType() {
        return OrbitType.KEPLERIAN;
    }

    /** {@inheritDoc} */
    @Override
    public double getA() {
        return this.parameters.getA();
    }

    /** {@inheritDoc} */
    @Override
    public double getE() {
        return this.parameters.getE();
    }

    /** {@inheritDoc} */
    @Override
    public double getI() {
        return this.parameters.getI();
    }

    /**
     * Get the perigee argument.
     * 
     * @return perigee argument (rad)
     */
    public double getPerigeeArgument() {
        return this.parameters.getPerigeeArgument();
    }

    /**
     * Get the right ascension of the ascending node.
     * 
     * @return right ascension of the ascending node (rad)
     */
    public double getRightAscensionOfAscendingNode() {
        return this.parameters.getRightAscensionOfAscendingNode();
    }

    /**
     * Get the anomaly.
     * 
     * @param type
     *        type of the angle
     * @return anomaly (rad)
     */
    public double getAnomaly(final PositionAngle type) {
        return this.parameters.getAnomaly(type);
    }

    /**
     * Get the true anomaly.
     * 
     * @return true anomaly (rad)
     */
    public double getTrueAnomaly() {
        return this.parameters.getTrueAnomaly();
    }

    /**
     * Get the eccentric anomaly.
     * 
     * @return eccentric anomaly (rad)
     */
    public double getEccentricAnomaly() {
        return this.parameters.getEccentricAnomaly();
    }

    /**
     * Get the mean anomaly.
     * 
     * @return mean anomaly (rad)
     */
    public double getMeanAnomaly() {
        return this.parameters.getMeanAnomaly();
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEx() {
        return this.parameters.getEquinoctialParameters().getEquinoctialEx();
    }

    /** {@inheritDoc} */
    @Override
    public double getEquinoctialEy() {
        return this.parameters.getEquinoctialParameters().getEquinoctialEy();
    }

    /** {@inheritDoc} */
    @Override
    public double getHx() {
        return this.parameters.getEquinoctialParameters().getHx();
    }

    /** {@inheritDoc} */
    @Override
    public double getHy() {
        return this.parameters.getEquinoctialParameters().getHy();
    }

    /** {@inheritDoc} */
    @Override
    public double getLv() {
        return this.parameters.getEquinoctialParameters().getLv();
    }

    /** {@inheritDoc} */
    @Override
    public double getLE() {
        return this.parameters.getEquinoctialParameters().getLE();
    }

    /** {@inheritDoc} */
    @Override
    public double getLM() {
        return this.parameters.getEquinoctialParameters().getLM();
    }

    /**
     * Get the mean motion.
     * 
     * @return mean motion (1/s)
     */
    @Override
    public double getN() {
        return this.parameters.getAlternateEquinoctialParameters().getN();
    }

    /** {@inheritDoc} */
    @Override
    protected PVCoordinates initPVCoordinates() {
        return this.parameters.getCartesianParameters().getPVCoordinates();
    }

    /** {@inheritDoc} */
    @Override
    protected KeplerianOrbit orbitShiftedBy(final double dt) {
        return new KeplerianOrbit(this.parameters.getA(), this.parameters.getE(), this.parameters.getI(),
            this.parameters.getPerigeeArgument(), this.parameters.getRightAscensionOfAscendingNode(),
            this.getMeanAnomaly() + this.getKeplerianMeanMotion() * dt, PositionAngle.MEAN, this.getFrame(),
            this.getDate().shiftedBy(dt), this.getMu());
    }

    /**
     * {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation on Keplerian elements, without
     * derivatives (which means the interpolation falls back to Lagrange interpolation only).
     * </p>
     */
    @Override
    public KeplerianOrbit interpolate(final AbsoluteDate date, final Collection<Orbit> sample) {

        // set up an interpolator
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        AbsoluteDate previousDate = null;
        double previousPA = Double.NaN;
        double previousRAAN = Double.NaN;
        double previousM = Double.NaN;
        for (final Orbit orbit : sample) {
            // Loop on samples
            final KeplerianOrbit kep = (KeplerianOrbit) OrbitType.KEPLERIAN.convertType(orbit);
            final double continuousPA;
            final double continuousRAAN;
            final double continuousM;
            if (previousDate == null) {
                continuousPA = kep.getPerigeeArgument();
                continuousRAAN = kep.getRightAscensionOfAscendingNode();
                continuousM = kep.getMeanAnomaly();
            } else {
                final double dt = kep.getDate().durationFrom(previousDate);
                final double keplerM = previousM + kep.getKeplerianMeanMotion() * dt;
                continuousPA = MathUtils.normalizeAngle(kep.getPerigeeArgument(), previousPA);
                continuousRAAN = MathUtils.normalizeAngle(kep.getRightAscensionOfAscendingNode(),
                    previousRAAN);
                continuousM = MathUtils.normalizeAngle(kep.getMeanAnomaly(), keplerM);
            }
            previousDate = kep.getDate();
            previousPA = continuousPA;
            previousRAAN = continuousRAAN;
            previousM = continuousM;
            // Add point
            interpolator
                .addSamplePoint(kep.getDate().durationFrom(date), new double[] { kep.getA(),
                    kep.getE(), kep.getI(), continuousPA, continuousRAAN, continuousM });
        }

        // interpolate
        final double[] interpolated = interpolator.value(0);

        // build a new interpolated instance
        return new KeplerianOrbit(interpolated[0], interpolated[1], interpolated[2],
            interpolated[3], interpolated[4], interpolated[5], PositionAngle.MEAN, this.getFrame(),
            date, this.getMu());

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianMeanWrtCartesian() {
        if (this.parameters.getA() > 0) {
            return this.computeJacobianMeanWrtCartesianElliptical();
        } else {
            return this.computeJacobianMeanWrtCartesianHyperbolic();
        }
    }

    /**
     * Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to cartesian coordinate
     * j (x for j=0, y for j=1, z for j=2, xDot for j=3, yDot for j=4, zDot for j=5).
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Orekit code kept as such
    private double[][] computeJacobianMeanWrtCartesianElliptical() {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CommentRatio check

        final double[][] jacobian = new double[6][6];

        final double a = this.parameters.getA();
        final double e = this.parameters.getE();
        final double i = this.parameters.getI();
        final double pa = this.parameters.getPerigeeArgument();

        // compute various intermediate parameters
        final PVCoordinates pvc = this.getPVCoordinates();
        final Vector3D position = pvc.getPosition();
        final Vector3D velocity = pvc.getVelocity();
        final Vector3D momentum = pvc.getMomentum();
        final double v2 = velocity.getNormSq();
        final double r2 = position.getNormSq();
        final double r = MathLib.sqrt(r2);
        final double r3 = r * r2;

        final double px = position.getX();
        final double py = position.getY();
        final double pz = position.getZ();
        final double vx = velocity.getX();
        final double vy = velocity.getY();
        final double vz = velocity.getZ();
        final double mx = momentum.getX();
        final double my = momentum.getY();
        final double mz = momentum.getZ();

        final double mu = this.getMu();
        final double sqrtMuA = MathLib.sqrt(a * mu);
        final double sqrtAoMu = MathLib.sqrt(a / mu);
        final double a2 = a * a;
        final double twoA = 2 * a;
        final double rOnA = r / a;

        final double oMe2 = 1 - e * e;
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, oMe2));
        final double sqrtRec = 1 / epsilon;

        final double[] sincosI = MathLib.sinAndCos(i);
        final double sinI = sincosI[0];
        final double cosI = sincosI[1];
        final double[] sincosPa = MathLib.sinAndCos(pa);
        final double sinPA = sincosPa[0];
        final double cosPA = sincosPa[1];

        final double pv = Vector3D.dotProduct(position, velocity);
        final double cosE = (a - r) / (a * e);
        final double sinE = pv / (e * sqrtMuA);

        // da
        final Vector3D vectorAR = new Vector3D(2 * a2 / r3, position);
        final Vector3D vectorARDot = velocity.scalarMultiply(2 * a2 / mu);
        fillHalfRow(1, vectorAR, jacobian[0], 0);
        fillHalfRow(1, vectorARDot, jacobian[0], 3);

        // de
        final double factorER3 = pv / twoA;
        final Vector3D vectorER = new Vector3D(cosE * v2 / (r * mu), position, sinE / sqrtMuA,
            velocity, -factorER3 * sinE / sqrtMuA, vectorAR);
        final Vector3D vectorERDot = new Vector3D(sinE / sqrtMuA, position, cosE * 2 * r / mu,
            velocity, -factorER3 * sinE / sqrtMuA, vectorARDot);
        fillHalfRow(1, vectorER, jacobian[1], 0);
        fillHalfRow(1, vectorERDot, jacobian[1], 3);

        // dE / dr (Eccentric anomaly)
        final double coefE = cosE / (e * sqrtMuA);
        final Vector3D vectorEAnR = new Vector3D(-sinE * v2 / (e * r * mu), position, coefE,
            velocity, -factorER3 * coefE, vectorAR);

        // dE / drDot
        final Vector3D vectorEAnRDot = new Vector3D(-sinE * 2 * r / (e * mu), velocity, coefE,
            position, -factorER3 * coefE, vectorARDot);

        // precomputing some more factors
        final double s1 = -sinE * pz / r - cosE * vz * sqrtAoMu;
        final double s2 = -cosE * pz / r3;
        final double s3 = -sinE * vz / (2 * sqrtMuA);
        final double t1 = sqrtRec * (cosE * pz / r - sinE * vz * sqrtAoMu);
        final double t2 = sqrtRec * (-sinE * pz / r3);
        final double t3 = sqrtRec * (cosE - e) * vz / (2 * sqrtMuA);
        final double t4 = sqrtRec * (e * sinI * cosPA * sqrtRec - vz * sqrtAoMu);
        final Vector3D s = new Vector3D(cosE / r, Vector3D.PLUS_K, s1, vectorEAnR, s2, position,
            s3, vectorAR);
        final Vector3D sDot = new Vector3D(-sinE * sqrtAoMu, Vector3D.PLUS_K, s1, vectorEAnRDot,
            s3, vectorARDot);
        final Vector3D t = new Vector3D(sqrtRec * sinE / r, Vector3D.PLUS_K).add(new Vector3D(t1,
            vectorEAnR, t2, position, t3, vectorAR, t4, vectorER));
        final Vector3D tDot = new Vector3D(sqrtRec * (cosE - e) * sqrtAoMu, Vector3D.PLUS_K, t1,
            vectorEAnRDot, t3, vectorARDot, t4, vectorERDot);

        // di
        final double factorI1 = -sinI * sqrtRec / sqrtMuA;
        final double i1 = factorI1;
        final double i2 = -factorI1 * mz / twoA;
        final double i3 = factorI1 * mz * e / oMe2;
        final double i4 = cosI * sinPA;
        final double i5 = cosI * cosPA;
        fillHalfRow(i1, new Vector3D(vy, -vx, 0), i2, vectorAR, i3, vectorER, i4, s, i5, t,
            jacobian[2], 0);
        fillHalfRow(i1, new Vector3D(-py, px, 0), i2, vectorARDot, i3, vectorERDot, i4, sDot, i5,
            tDot, jacobian[2], 3);

        // dpa
        fillHalfRow(cosPA / sinI, s, -sinPA / sinI, t, jacobian[3], 0);
        fillHalfRow(cosPA / sinI, sDot, -sinPA / sinI, tDot, jacobian[3], 3);

        // dRaan
        final double factorRaanR = 1 / (mu * a * oMe2 * sinI * sinI);
        fillHalfRow(-factorRaanR * my, new Vector3D(0, vz, -vy), factorRaanR * mx, new Vector3D(
            -vz, 0, vx), jacobian[4], 0);
        fillHalfRow(-factorRaanR * my, new Vector3D(0, -pz, py), factorRaanR * mx, new Vector3D(pz,
            0, -px), jacobian[4], 3);

        // dM
        fillHalfRow(rOnA, vectorEAnR, -sinE, vectorER, jacobian[5], 0);
        fillHalfRow(rOnA, vectorEAnRDot, -sinE, vectorERDot, jacobian[5], 3);

        return jacobian;

    }

    /**
     * Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to cartesian coordinate
     * j (x for j=0, y for j=1, z for j=2, xDot for j=3, yDot for j=4, zDot for j=5).
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     */
    // CHECKSTYLE: stop CommentRatio check
    // CHECKSTYLE: stop MethodLength check
    // Reason: Orekit code kept as such
    private double[][] computeJacobianMeanWrtCartesianHyperbolic() {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CommentRatio check

        final double[][] jacobian = new double[6][6];

        final double a = this.parameters.getA();
        final double e = this.parameters.getE();
        final double i = this.parameters.getI();

        // compute various intermediate parameters
        final PVCoordinates pvc = this.getPVCoordinates();
        final Vector3D position = pvc.getPosition();
        final Vector3D velocity = pvc.getVelocity();
        final Vector3D momentum = pvc.getMomentum();
        final double r2 = position.getNormSq();
        final double r = MathLib.sqrt(r2);
        final double r3 = r * r2;

        final double x = position.getX();
        final double y = position.getY();
        final double z = position.getZ();
        final double vx = velocity.getX();
        final double vy = velocity.getY();
        final double vz = velocity.getZ();
        final double mx = momentum.getX();
        final double my = momentum.getY();
        final double mz = momentum.getZ();

        final double mu = this.getMu();
        final double absA = -a;
        final double sqrtMuA = MathLib.sqrt(absA * mu);
        final double a2 = a * a;
        final double rOa = r / absA;

        final double[] sincosI = MathLib.sinAndCos(i);
        final double sinI = sincosI[0];
        final double cosI = sincosI[1];

        final double pv = Vector3D.dotProduct(position, velocity);

        // da
        final Vector3D vectorAR = new Vector3D(-2 * a2 / r3, position);
        final Vector3D vectorARDot = velocity.scalarMultiply(-2 * a2 / mu);
        fillHalfRow(-1, vectorAR, jacobian[0], 0);
        fillHalfRow(-1, vectorARDot, jacobian[0], 3);

        // differentials of the momentum
        final double m = momentum.getNorm();
        final double oOm = 1 / m;
        final Vector3D dcXP = new Vector3D(0, vz, -vy);
        final Vector3D dcYP = new Vector3D(-vz, 0, vx);
        final Vector3D dcZP = new Vector3D(vy, -vx, 0);
        final Vector3D dcXV = new Vector3D(0, -z, y);
        final Vector3D dcYV = new Vector3D(z, 0, -x);
        final Vector3D dcZV = new Vector3D(-y, x, 0);
        final Vector3D dCP = new Vector3D(mx * oOm, dcXP, my * oOm, dcYP, mz * oOm, dcZP);
        final Vector3D dCV = new Vector3D(mx * oOm, dcXV, my * oOm, dcYV, mz * oOm, dcZV);

        // dp
        final double mOMu = m / mu;
        final Vector3D dpP = new Vector3D(2 * mOMu, dCP);
        final Vector3D dpV = new Vector3D(2 * mOMu, dCV);

        // de
        final double p = m * mOMu;
        final double moO2ae = 1 / (2 * absA * e);
        final double m2OaMu = -p / absA;
        fillHalfRow(moO2ae, dpP, m2OaMu * moO2ae, vectorAR, jacobian[1], 0);
        fillHalfRow(moO2ae, dpV, m2OaMu * moO2ae, vectorARDot, jacobian[1], 3);

        // di
        final double cI1 = 1 / (m * sinI);
        final double cI2 = cosI * cI1;
        fillHalfRow(cI2, dCP, -cI1, dcZP, jacobian[2], 0);
        fillHalfRow(cI2, dCV, -cI1, dcZV, jacobian[2], 3);

        // dPA
        final double cP1 = y * oOm;
        final double cP2 = -x * oOm;
        final double cP3 = -(mx * cP1 + my * cP2);
        final double cP4 = cP3 * oOm;
        final double cP5 = -1 / (r2 * sinI * sinI);
        final double cP6 = z * cP5;
        final double cP7 = cP3 * cP5;
        final Vector3D dacP = new Vector3D(cP1, dcXP, cP2, dcYP, cP4, dCP, oOm, new Vector3D(-my,
            mx, 0));
        final Vector3D dacV = new Vector3D(cP1, dcXV, cP2, dcYV, cP4, dCV);
        final Vector3D dpoP = new Vector3D(cP6, dacP, cP7, Vector3D.PLUS_K);
        final Vector3D dpoV = new Vector3D(cP6, dacV);

        final double re2 = r2 * e * e;
        final double recOre2 = (p - r) / re2;
        final double resOre2 = (pv * mOMu) / re2;
        final Vector3D dreP = new Vector3D(mOMu, velocity, pv / mu, dCP);
        final Vector3D dreV = new Vector3D(mOMu, position, pv / mu, dCV);
        final Vector3D davP = new Vector3D(-resOre2, dpP, recOre2, dreP, resOre2 / r, position);
        final Vector3D davV = new Vector3D(-resOre2, dpV, recOre2, dreV);
        fillHalfRow(1, dpoP, -1, davP, jacobian[3], 0);
        fillHalfRow(1, dpoV, -1, davV, jacobian[3], 3);

        // dRAAN
        final double cO0 = cI1 * cI1;
        final double cO1 = mx * cO0;
        final double cO2 = -my * cO0;
        fillHalfRow(cO1, dcYP, cO2, dcXP, jacobian[4], 0);
        fillHalfRow(cO1, dcYV, cO2, dcXV, jacobian[4], 3);

        // dM
        final double s2a = pv / (2 * absA);
        final double oObux = 1 / MathLib.sqrt(m * m + mu * absA);
        final double scasbu = pv * oObux;
        final Vector3D dauP = new Vector3D(1 / sqrtMuA, velocity, -s2a / sqrtMuA, vectorAR);
        final Vector3D dauV = new Vector3D(1 / sqrtMuA, position, -s2a / sqrtMuA, vectorARDot);
        final Vector3D dbuP = new Vector3D(oObux * mu / 2, vectorAR, m * oObux, dCP);
        final Vector3D dbuV = new Vector3D(oObux * mu / 2, vectorARDot, m * oObux, dCV);
        final Vector3D dcuP = new Vector3D(oObux, velocity, -scasbu * oObux, dbuP);
        final Vector3D dcuV = new Vector3D(oObux, position, -scasbu * oObux, dbuV);
        fillHalfRow(1, dauP, -e / (1 + rOa), dcuP, jacobian[5], 0);
        fillHalfRow(1, dauV, -e / (1 + rOa), dcuV, jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianEccentricWrtCartesian() {
        if (this.parameters.getA() > 0) {
            return this.computeJacobianEccentricWrtCartesianElliptical();
        } else {
            return this.computeJacobianEccentricWrtCartesianHyperbolic();
        }
    }

    /**
     * Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to cartesian coordinate
     * j (x for j=0, y for j=1, z for j=2, xDot for j=3, yDot for j=4, zDot for j=5).
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianEccentricWrtCartesianElliptical() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = this.computeJacobianMeanWrtCartesianElliptical();

        final double e = this.parameters.getE();

        // Differentiating the Kepler equation M = E - e sin E leads to:
        // dM = (1 - e cos E) dE - sin E de
        // which is inverted and rewritten as:
        // dE = a/r dM + sin E a/r de
        final double eccentricAnomaly = this.getEccentricAnomaly();
        final double[] sincos = MathLib.sinAndCos(eccentricAnomaly);
        final double sinE = sincos[0];
        final double cosE = sincos[1];
        final double aOr = 1 / (1 - e * cosE);

        // update anomaly row
        final double[] eRow = jacobian[1];
        final double[] anomalyRow = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aOr * (anomalyRow[j] + sinE * eRow[j]);
        }

        return jacobian;

    }

    /**
     * Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to cartesian coordinate
     * j (x for j=0, y for j=1, z for j=2, xDot for j=3, yDot for j=4, zDot for j=5).
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianEccentricWrtCartesianHyperbolic() {

        // start by computing the Jacobian with mean angle
        final double[][] jacobian = this.computeJacobianMeanWrtCartesianHyperbolic();

        final double e = this.parameters.getE();

        // Differentiating the Kepler equation M = e sinh H - H leads to:
        // dM = (e cosh H - 1) dH + sinh H de
        // which is inverted and rewritten as:
        // dH = 1 / (e cosh H - 1) dM - sinh H / (e cosh H - 1) de
        final double h = this.getEccentricAnomaly();
        final double[] sinhcosh = MathLib.sinhAndCosh(h);
        final double sinhH = sinhcosh[0];
        final double coshH = sinhcosh[1];
        final double absaOr = 1 / (e * coshH - 1);

        // update anomaly row
        final double[] eRow = jacobian[1];
        final double[] anomalyRow = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = absaOr * (anomalyRow[j] - sinhH * eRow[j]);
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    protected double[][] computeJacobianTrueWrtCartesian() {
        if (this.parameters.getA() > 0) {
            return this.computeJacobianTrueWrtCartesianElliptical();
        } else {
            return this.computeJacobianTrueWrtCartesianHyperbolic();
        }
    }

    /**
     * Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to cartesian coordinate
     * j (x for j=0, y for j=1, z for j=2, xDot for j=3, yDot for j=4, zDot for j=5).
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianTrueWrtCartesianElliptical() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = this.computeJacobianEccentricWrtCartesianElliptical();

        final double e = this.parameters.getE();

        // Differentiating the eccentric anomaly equation sin E = sqrt(1-e^2) sin v / (1 + e cos v)
        // and using cos E = (e + cos v) / (1 + e cos v) to get rid of cos E leads to:
        // dE = [sqrt (1 - e^2) / (1 + e cos v)] dv - [sin E / (1 - e^2)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dE + [sin E / sqrt (1 - e^2)] a/r de
        final double e2 = e * e;
        final double oMe2 = 1 - e2;
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, oMe2));
        final double eccentricAnomaly = this.getEccentricAnomaly();
        final double[] sincosE = MathLib.sinAndCos(eccentricAnomaly);
        final double sinE = sincosE[0];
        final double cosE = sincosE[1];
        final double aOr = 1 / (1 - e * cosE);
        final double aFactor = epsilon * aOr;
        final double eFactor = sinE * aOr / epsilon;

        // update anomaly row
        final double[] eRow = jacobian[1];
        final double[] anomalyRow = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor * anomalyRow[j] + eFactor * eRow[j];
        }

        return jacobian;

    }

    /**
     * Compute the Jacobian of the orbital parameters with respect to the cartesian parameters.
     * <p>
     * Element {@code jacobian[i][j]} is the derivative of parameter i of the orbit with respect to cartesian coordinate
     * j (x for j=0, y for j=1, z for j=2, xDot for j=3, yDot for j=4, zDot for j=5).
     * </p>
     * 
     * @return 6x6 Jacobian matrix
     */
    private double[][] computeJacobianTrueWrtCartesianHyperbolic() {

        // start by computing the Jacobian with eccentric angle
        final double[][] jacobian = this.computeJacobianEccentricWrtCartesianHyperbolic();

        final double e = this.parameters.getE();

        // Differentiating the eccentric anomaly equation sinh H = sqrt(e^2-1) sin v /
        // (1 + e cos v)
        // and using cosh H = (e + cos v) / (1 + e cos v) to get rid of cosh H leads to:
        // dH = [sqrt (e^2 - 1) / (1 + e cos v)] dv + [sinh H / (e^2 - 1)] de
        // which is inverted and rewritten as:
        // dv = sqrt (1 - e^2) a/r dH - [sinh H / sqrt (e^2 - 1)] a/r de
        final double e2 = e * e;
        final double e2Mo = e2 - 1;
        final double epsilon = MathLib.sqrt(MathLib.max(0.0, e2Mo));
        final double h = this.getEccentricAnomaly();
        final double[] sinhcosh = MathLib.sinhAndCosh(h);
        final double sinhH = sinhcosh[0];
        final double coshH = sinhcosh[1];
        final double aOr = 1 / (e * coshH - 1);
        final double aFactor = epsilon * aOr;
        final double eFactor = sinhH * aOr / epsilon;

        // update anomaly row
        final double[] eRow = jacobian[1];
        final double[] anomalyRow = jacobian[5];
        for (int j = 0; j < anomalyRow.length; ++j) {
            anomalyRow[j] = aFactor * anomalyRow[j] - eFactor * eRow[j];
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.parameters.toString();
    }

    /** {@inheritDoc} */
    @Override
	public boolean equals(final Object object) {
		// parameters : date, frame, type, mu,
		//              a, e, i, Right Ascension of the Ascending Node, Perigee Argument,
		//              true anomaly
        boolean isEqual = true;
		
        if (object == this) { 
            // first fast check
            isEqual = true; 
        } else if (object instanceof KeplerianOrbit) {
            // cast object to compare parameters
            final KeplerianOrbit other = (KeplerianOrbit) object;
        	
            isEqual &= (this.getDate().equals(other.getDate()));
            isEqual &= (this.getFrame().equals(other.getFrame()));
            isEqual &= (this.parameters.equals(other.parameters));
        	
        	
        } else {
            isEqual = false;
        }
		
        return isEqual;
    }

	/** {@inheritDoc} */
    @Override
	public int hashCode() {
        // A not zero random "root int"
        int result = ROOTINT;
        // An efficient multiplier (JVM optimizes 31 * i as (i << 5) - 1 )
        final int effMult = 31;
        // Good hashcode : it's the same
        // for "equal" orbits, but
        // reasonably sure it's different otherwise.
        result = effMult * result + this.getDate().hashCode();
        result = effMult * result + this.getFrame().hashCode();
        result = effMult * result + this.parameters.hashCode();
 
        return result; 
    }

}
