/**
 * Copyright 2011-2022 CNES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.bodies.EllipsoidBodyShape;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

// CHECKSTYLE: stop MagicNumber check
// Reason: model - Orekit code

/**
 * This atmosphere model is the realization of the DTM-2012 model.
 * <p>
 * This class is restricted to be used with {@link EllipsoidBodyShape}.
 * </p>
 * 
 * @author Williams Zanga
 *
 * @since 4.6
 */
public class DTM2012 extends AbstractDTM {

    /** Serializable UID. */
    private static final long serialVersionUID = -3914977678649121180L;

    /** Resources text file. */
    private static final String DATA_FILE_DTM2012 = "/META-INF/dtm_2013.txt";

    /** Number of one SUM(x*dx) except dPhas components **/
    private static final int NB_ONE = 1;

    /** Index of one components in the global arrays */
    private static final int INDEX_ONE = NB_ONE - 1;

    /** Number of latitude components */
    private static final int NB_LAT = 6;

    /** Index of latitude components in the global arrays */
    private static final int INDEX_LAT = INDEX_ONE + NB_ONE;

    /** Number of flux components */
    private static final int NB_FLUX = 12;

    /** Index of flux components in the global arrays */
    private static final int INDEX_FLUX = INDEX_LAT + NB_LAT;

    /** Number of kp components */
    private static final int NB_KP = 15;

    /** Index of kp components in the global arrays */
    private static final int INDEX_KP = INDEX_FLUX + NB_FLUX;

    /** Number of SLat (annual & symetric in latitude) components */
    private static final int NB_SLAT = 2;

    /** Index of SLat components in the global arrays */
    private static final int INDEX_SLAT = INDEX_KP + NB_KP;

    /** Number of SASlat (semi-annual & symetric in latitude) components */
    private static final int NB_SASLAT = 2;

    /** Index of SASlat components in the global arrays */
    private static final int INDEX_SASLAT = INDEX_SLAT + NB_SLAT;

    /** Number of NSlat (annual & non-symetric in latitude) components */
    private static final int NB_NSLAT = 3;

    /** Index of NSlat components in the global arrays */
    private static final int INDEX_NSLAT = INDEX_SASLAT + NB_SASLAT;

    /** Number of SANSlat (semi-annual non-symetric in latitude) components */
    private static final int NB_SANSLAT = 3;

    /** Index of SANSlat components in the global arrays */
    private static final int INDEX_SANSLAT = NB_NSLAT + INDEX_NSLAT;

    /** Number of DiAn (diurnal & annual coupled) components */
    private static final int NB_DIAN = 12;

    /** Index of DiAn components in the global arrays */
    private static final int INDEX_DIAN = INDEX_SANSLAT + NB_SANSLAT;

    /** Number of SDiAn (semi-diurnal & annual coupled) components */
    private static final int NB_SDIAN = 12;

    /** Index of SDiAn components in the global arrays */
    private static final int INDEX_SDIAN = INDEX_DIAN + NB_DIAN;

    /** Number of TDi (ter-diurnal) components */
    private static final int NB_TDI = 2;

    /** Index of TDi components in the global arrays */
    private static final int INDEX_TDI = INDEX_SDIAN + NB_SDIAN;

    /** Number of AMg (activity Magnetic) components **/
    private static final int NB_AMG = 9;

    /** Index of AMg components in the global arrays */
    private static final int INDEX_AMG = INDEX_TDI + NB_TDI;

    /** Number of longitude components **/
    private static final int NB_LON = 10;

    /** Index of longitude components in the global arrays */
    private static final int INDEX_LON = INDEX_AMG + NB_AMG;

    /** Index of dPhas components in the global arrays */
    private static final int INDEX_DPHAS = INDEX_LON + NB_LON;

    /**
     * Constructor with user-provided data file.
     * 
     * @param parameters
     *        the solar and magnetic activity data
     * @param sunIn
     *        the sun position
     * @param earthIn
     *        the earth body shape
     * @param dataFilename
     *        data filename
     * @throws PatriusException
     *         if some resource file reading error occurs
     */
    public DTM2012(final DTMInputParameters parameters, final PVCoordinatesProvider sunIn,
                   final EllipsoidBodyShape earthIn, final String dataFilename) throws PatriusException {
        super(parameters, sunIn, earthIn, dataFilename);
    }

    /**
     * Simple constructor with PATRIUS data file.
     * 
     * @param parameters
     *        the solar and magnetic activity data
     * @param sunIn
     *        the sun position
     * @param earthIn
     *        the earth body shape
     * @throws PatriusException
     *         if some resource file reading error occurs
     */
    public DTM2012(final DTMInputParameters parameters, final PVCoordinatesProvider sunIn,
                   final EllipsoidBodyShape earthIn) throws PatriusException {
        this(parameters, sunIn, earthIn, DATA_FILE_DTM2012);
    }

    /** {@inheritDoc} */
    @Override
    protected void densityComputationFromFortran() {
        // Initialization
        this.ro = 0.0;
        final int kleq = Integer.MIN_VALUE;
        final double zlb = ZLB0;

        // compute geographic and magnetic pole coefficients with Legendre polynomials
        computeGeographicMagneticCoefficients();

        // compute function g(l) / tinf, t120, tp120
        final double gdelt = this.gFunction(this.tt, this.dtt, 1, kleq);
        this.dtt[1] = 1.0 + gdelt;
        this.tinf = this.tt[1] * this.dtt[1];

        final double gdelt0 = this.gFunction(this.t0, this.dt0, 1, kleq);
        this.dt0[1] = 1. + gdelt0;
        final double t120 = this.t0[1] * this.dt0[1];

        final double gdeltp = this.gFunction(this.tp, this.dtp, 1, kleq);
        this.dtp[1] = 1. + gdeltp;
        final double tp120 = this.tp[1] * this.dtp[1];

        // compute n(z) concentrations:
        // H, He, O, N2, O2, N
        final double sigma = tp120 / (this.tinf - t120);
        final double dzeta = (RE + zlb) / (RE + this.cachedAlti);
        final double zeta = (this.cachedAlti - zlb) * dzeta;
        final double sigzeta = sigma * zeta;
        final double expsz = MathLib.exp(-sigzeta);
        this.cachedTemperature = this.tinf - (this.tinf - t120) * expsz;

        // Base density
        final double[] dbase = new double[7];

        final double gdelh = this.gFunction(this.h, this.dh, 0, kleq);
        this.dh[1] = MathLib.exp(gdelh);
        dbase[1] = this.h[1] * this.dh[1];

        final double gdelhe = this.gFunction(this.he, this.dhe, 0, kleq);
        this.dhe[1] = MathLib.exp(gdelhe);
        dbase[2] = this.he[1] * this.dhe[1];

        final double gdelo = this.gFunction(this.o, this.dox, 1, kleq);
        this.dox[1] = MathLib.exp(gdelo);
        dbase[3] = this.o[1] * this.dox[1];

        final double gdelaz2 = this.gFunction(this.az2, this.daz2, 1, kleq);
        this.daz2[1] = MathLib.exp(gdelaz2);
        dbase[4] = this.az2[1] * this.daz2[1];

        final double gdelo2 = this.gFunction(this.o2, this.do2, 1, kleq);
        this.do2[1] = MathLib.exp(gdelo2);
        dbase[5] = this.o2[1] * this.do2[1];

        final double gdelaz = this.gFunction(this.az, this.daz, 1, kleq);
        this.daz[1] = MathLib.exp(gdelaz);
        dbase[6] = this.az[1] * this.daz[1];

        final double zlbre = 1.0 + zlb / RE;
        final double glb = (GSURF / (zlbre * zlbre)) / (sigma * RGAS * this.tinf);
        final double t120tz = t120 / this.cachedTemperature;

        // Initialize fz
        final double[] fz = new double[7];

        // Loop on all components
        final double[] d = new double[7];
        for (int i = 1; i <= 6; i++) {
            final double gamma = MA[i] * glb;
            final double upapg = 1.0 + ALEFA[i] + gamma;
            fz[i] = MathLib.pow(t120tz, upapg) * MathLib.exp(-sigzeta * gamma);
            // concentrations of H, He, O, N2, O2, N (particles/cm<sup>3</sup>)
            this.cc[i] = dbase[i] * fz[i];
            // densities of H, He, O, N2, O2, N (g/cm<sup>3</sup>)
            d[i] = this.cc[i] * VMA[i];
            // total density
            this.ro += d[i];
        }

        // average of atomic mass
        // final double wmm=this.ro/(VMA[1]*(this.cc[1]+this.cc[2]+this.cc[3]+this.cc[4]+this.cc[5]+this.cc[6]));
    }

    /**
     * {@inheritDoc}
     * <p>
     * S.Bruisma 06/03/2009 MOD PS 04/2012 : rol calculation of function g(l) for dtm2009 & dtm2012. (Java
     * implementation from the original Fortran routine)
     * </p>
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Fortran code kept as such
    @Override
    protected double gFunction(final double[] a, final double[] da, final int ff0, final int kleEq) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CommentRatio check

        // local scalars
        final double f1f;
        final double c2fi;
        final double clfl;
        final double cos2te;
        final double coste;
        final double dakpm;
        final double dkp;
        final double dkpm;
        final double slfl;
        double dakp;
        double f0;
        double fp;
        final double[] fbm150 = new double[3];
        final double[] fmfb = new double[3];

        // terms in latitude
        da[INDEX_LAT + 1] = this.p20;
        da[INDEX_LAT + 2] = this.p40;
        da[INDEX_LAT + 3] = this.p10;
        da[INDEX_LAT + 4] = this.p30;
        da[INDEX_LAT + 5] = this.p50;
        da[INDEX_LAT + 6] = this.p60;

        // terms of flux
        fmfb[1] = this.cachedF[1] - this.cachedFbar[1];
        fmfb[2] = this.cachedF[2] - this.cachedFbar[2];
        fbm150[1] = this.cachedFbar[1] - 150.0;
        fbm150[2] = this.cachedFbar[2];

        da[INDEX_FLUX + 1] = fmfb[1];
        da[INDEX_FLUX + 3] = fbm150[1];

        da[INDEX_FLUX + 1] += a[INDEX_FLUX + 5] * fmfb[2];
        da[INDEX_FLUX + 3] += a[INDEX_FLUX + 6] * fbm150[2];

        da[INDEX_FLUX + 5] = fmfb[2]
                * (a[INDEX_FLUX + 1] + 2. * a[INDEX_FLUX + 2] * da[INDEX_FLUX + 1] + a[INDEX_FLUX + 7] * this.p10
                        + a[INDEX_FLUX + 8] * this.p20 + a[INDEX_FLUX + 9] * this.p30);

        da[INDEX_FLUX + 6] = fbm150[2]
                * (a[INDEX_FLUX + 3] + 2. * a[INDEX_FLUX + 4] * da[INDEX_FLUX + 3] + a[INDEX_FLUX + 10] * this.p10
                        + a[INDEX_FLUX + 11] * this.p20 + a[INDEX_FLUX + 12] * this.p30);

        da[INDEX_FLUX + 2] = da[INDEX_FLUX + 1] * da[INDEX_FLUX + 1];
        da[INDEX_FLUX + 4] = da[INDEX_FLUX + 3] * da[INDEX_FLUX + 3];
        da[INDEX_FLUX + 7] = da[INDEX_FLUX + 1] * this.p10;
        da[INDEX_FLUX + 8] = da[INDEX_FLUX + 1] * this.p20;
        da[INDEX_FLUX + 9] = da[INDEX_FLUX + 1] * this.p30;
        da[INDEX_FLUX + 10] = da[INDEX_FLUX + 3] * this.p20;
        da[INDEX_FLUX + 11] = da[INDEX_FLUX + 3] * this.p30;
        da[INDEX_FLUX + 12] = da[INDEX_FLUX + 3] * this.p40;

        // terms in kp
        c2fi = 1. - this.p10mg * this.p10mg;
        dkp = this.akp[1] + (a[INDEX_KP + 5] + c2fi * a[INDEX_KP + 6]) * this.akp[2];

        dakp = a[INDEX_KP + 1] + a[INDEX_KP + 2] * this.p20mg + a[INDEX_KP + 11] * this.p40mg + 2.0 * dkp
                * (a[INDEX_KP + 3] + a[INDEX_KP + 4] * this.p20mg + a[INDEX_KP + 14] * 2.0 * dkp * dkp);

        da[INDEX_KP + 5] = dakp * this.akp[2];
        da[INDEX_KP + 6] = da[INDEX_KP + 5] * c2fi;

        dkpm = this.akp[3] + a[INDEX_KP + 10] * this.akp[4];
        dakpm = a[INDEX_KP + 7] + a[INDEX_KP + 8] * this.p20mg + a[INDEX_KP + 12] * this.p40mg + 2.0 * dkpm
                * (a[INDEX_KP + 9] + a[INDEX_KP + 13] * this.p20mg + a[INDEX_KP + 15] * 2.0 * dkpm * dkpm);

        da[INDEX_KP + 10] = dakpm * this.akp[4];

        da[INDEX_KP + 1] = dkp;
        da[INDEX_KP + 2] = this.p20mg * dkp;
        da[INDEX_KP + 11] = this.p40mg * dkp;
        da[INDEX_KP + 3] = dkp * dkp;
        da[INDEX_KP + 4] = this.p20mg * da[INDEX_KP + 3];
        da[INDEX_KP + 14] = da[INDEX_KP + 3] * da[INDEX_KP + 3];
        da[INDEX_KP + 7] = dkpm;
        da[INDEX_KP + 8] = this.p20mg * dkpm;
        da[INDEX_KP + 12] = this.p40mg * dkpm;
        da[INDEX_KP + 9] = dkpm * dkpm;
        da[INDEX_KP + 13] = this.p20mg * da[INDEX_KP + 9];
        da[INDEX_KP + 15] = da[INDEX_KP + 9] * da[INDEX_KP + 9];

        // function g(l) periodic
        f0 = a[INDEX_FLUX + 1] * da[INDEX_FLUX + 1] + a[INDEX_FLUX + 2] * da[INDEX_FLUX + 2] + a[INDEX_FLUX + 3]
                * da[INDEX_FLUX + 3] + a[INDEX_FLUX + 4] * da[INDEX_FLUX + 4] + a[INDEX_FLUX + 7] * da[INDEX_FLUX + 7]
                + a[INDEX_FLUX + 8] * da[INDEX_FLUX + 8] + a[INDEX_FLUX + 9] * da[INDEX_FLUX + 9] + a[INDEX_FLUX + 10]
                * da[INDEX_FLUX + 10] + a[INDEX_FLUX + 11] * da[INDEX_FLUX + 11] + a[INDEX_FLUX + 12]
                * da[INDEX_FLUX + 12];

        f1f = 1. + f0 * ff0;

        f0 += a[INDEX_LAT + 1] * da[INDEX_LAT + 1] + a[INDEX_LAT + 2] * da[INDEX_LAT + 2] + a[INDEX_LAT + 3]
                * da[INDEX_LAT + 3] + a[INDEX_LAT + 4] * da[INDEX_LAT + 4] + a[INDEX_KP + 1] * da[INDEX_KP + 1]
                + a[INDEX_KP + 2] * da[INDEX_KP + 2] + a[INDEX_KP + 3] * da[INDEX_KP + 3] + a[INDEX_KP + 4]
                * da[INDEX_KP + 4] + a[INDEX_KP + 11] * da[INDEX_KP + 11] + a[INDEX_KP + 7] * da[INDEX_KP + 7]
                + a[INDEX_KP + 8] * da[INDEX_KP + 8] + a[INDEX_KP + 9] * da[INDEX_KP + 9] + a[INDEX_KP + 12]
                * da[INDEX_KP + 12] + a[INDEX_KP + 13] * da[INDEX_KP + 13] + a[INDEX_KP + 14] * da[INDEX_KP + 14]
                + a[INDEX_KP + 15] * da[INDEX_KP + 15] + a[INDEX_LAT + 5] * da[INDEX_LAT + 5] + a[INDEX_LAT + 6]
                * da[INDEX_LAT + 6];

        // terms annual & symetric in latitude
        da[INDEX_SLAT + 1] = MathLib.cos(ROT * (this.cachedDay - a[INDEX_DPHAS + 1]));
        da[INDEX_SLAT + 2] = this.p20 * da[INDEX_SLAT + 1];

        // terms semi-annual & symetric in latitude
        da[INDEX_SASLAT + 1] = MathLib.cos(ROT2 * (this.cachedDay - a[INDEX_DPHAS + 2]));
        da[INDEX_SASLAT + 2] = this.p20 * da[INDEX_SASLAT + 1];

        // terms annual & non-symetric in latitude
        coste = MathLib.cos(ROT * (this.cachedDay - a[INDEX_DPHAS + 3]));
        da[INDEX_NSLAT + 1] = this.p10 * coste;
        da[INDEX_NSLAT + 2] = this.p30 * coste;
        da[INDEX_NSLAT + 3] = da[INDEX_FLUX + 3] * da[INDEX_NSLAT + 1];
        // da[INDEX_NSLAT+3] = p50 * coste

        // terms semi-annual non-symetric in latitude
        cos2te = MathLib.cos(ROT2 * (this.cachedDay - a[INDEX_DPHAS + 4]));
        da[INDEX_SANSLAT + 1] = this.p10 * cos2te;
        da[INDEX_SANSLAT + 2] = this.p30 * cos2te;
        da[INDEX_SANSLAT + 3] = da[INDEX_FLUX + 3] * da[INDEX_SANSLAT + 1];
        // da[getGlobalIndex("SANSLat",2)] = p50 * cosete

        // terms diurnal (& annual coupled)
        da[INDEX_DIAN + 1] = this.p11 * this.ch;
        da[INDEX_DIAN + 2] = this.p31 * this.ch;
        da[INDEX_DIAN + 3] = da[INDEX_FLUX + 3] * da[INDEX_DIAN + 1];
        da[INDEX_DIAN + 4] = da[INDEX_DIAN + 1] * coste;
        da[INDEX_DIAN + 5] = this.p21 * this.ch * coste;
        da[INDEX_DIAN + 6] = this.p11 * this.sh;
        da[INDEX_DIAN + 7] = this.p31 * this.sh;
        da[INDEX_DIAN + 8] = da[INDEX_FLUX + 3] * da[INDEX_DIAN + 6];
        da[INDEX_DIAN + 9] = da[INDEX_DIAN + 6] * coste;
        da[INDEX_DIAN + 10] = this.p21 * this.sh * coste;
        da[INDEX_DIAN + 11] = this.p51 * this.ch;
        da[INDEX_DIAN + 12] = this.p51 * this.sh;

        // terms semi-diurnes (& annual coupled)
        da[INDEX_SDIAN + 1] = this.p22 * this.c2h;
        da[INDEX_SDIAN + 2] = this.p42 * this.c2h;
        da[INDEX_SDIAN + 3] = this.p32 * this.c2h * coste;
        da[INDEX_SDIAN + 4] = this.p22 * this.s2h;
        da[INDEX_SDIAN + 5] = this.p42 * this.s2h;
        da[INDEX_SDIAN + 6] = this.p32 * this.s2h * coste;
        da[INDEX_SDIAN + 7] = this.p32 * this.c2h; // coeff. rajoute pour tp120/t120 (slb)
        da[INDEX_SDIAN + 8] = this.p32 * this.s2h;
        da[INDEX_SDIAN + 9] = da[INDEX_FLUX + 3] * da[INDEX_SDIAN + 1];
        da[INDEX_SDIAN + 10] = da[INDEX_FLUX + 3] * da[INDEX_SDIAN + 4];
        da[INDEX_SDIAN + 11] = this.p62 * this.c2h;
        da[INDEX_SDIAN + 12] = this.p62 * this.s2h;

        // terms ter-diurnes
        da[INDEX_TDI + 1] = this.p33 * this.c3h;
        da[INDEX_TDI + 2] = this.p33 * this.s3h;

        // function periodic -> g(l)
        fp = a[INDEX_SLAT + 1] * da[INDEX_SLAT + 1] + a[INDEX_SLAT + 2] * da[INDEX_SLAT + 2]
                + a[INDEX_SASLAT + 1] * da[INDEX_SASLAT + 1] + a[INDEX_SASLAT + 2] * da[INDEX_SASLAT + 2]
                + a[INDEX_NSLAT + 1] * da[INDEX_NSLAT + 1] + a[INDEX_NSLAT + 2] * da[INDEX_NSLAT + 2]
                + a[INDEX_NSLAT + 3] * da[INDEX_NSLAT + 3] + a[INDEX_SANSLAT + 1] * da[INDEX_SANSLAT + 1]
                + a[INDEX_DIAN + 1] * da[INDEX_DIAN + 1] + a[INDEX_DIAN + 2] * da[INDEX_DIAN + 2]
                + a[INDEX_DIAN + 3] * da[INDEX_DIAN + 3] + a[INDEX_DIAN + 4] * da[INDEX_DIAN + 4]
                + a[INDEX_DIAN + 5] * da[INDEX_DIAN + 5] + a[INDEX_DIAN + 6] * da[INDEX_DIAN + 6]
                + a[INDEX_DIAN + 7] * da[INDEX_DIAN + 7] + a[INDEX_DIAN + 8] * da[INDEX_DIAN + 8]
                + a[INDEX_DIAN + 9] * da[INDEX_DIAN + 9] + a[INDEX_DIAN + 10] * da[INDEX_DIAN + 10]
                + a[INDEX_SDIAN + 1] * da[INDEX_SDIAN + 1] + a[INDEX_SDIAN + 3] * da[INDEX_SDIAN + 3]
                + a[INDEX_SDIAN + 4] * da[INDEX_SDIAN + 4] + a[INDEX_SDIAN + 6] * da[INDEX_SDIAN + 6]
                + a[INDEX_TDI + 1] * da[INDEX_TDI + 1] + a[INDEX_TDI + 2] * da[INDEX_TDI + 2] + a[INDEX_SDIAN + 2]
                * da[INDEX_SDIAN + 2] + a[INDEX_SDIAN + 5] * da[INDEX_SDIAN + 5] + a[INDEX_SANSLAT + 2]
                * da[INDEX_SANSLAT + 2] + a[INDEX_SANSLAT + 3] * da[INDEX_SANSLAT + 3] + a[INDEX_SDIAN + 7]
                * da[INDEX_SDIAN + 7] + a[INDEX_SDIAN + 8] * da[INDEX_SDIAN + 8] + a[INDEX_SDIAN + 9]
                * da[INDEX_SDIAN + 9] + a[INDEX_SDIAN + 10] * da[INDEX_SDIAN + 10] + a[INDEX_SDIAN + 11]
                * da[INDEX_SDIAN + 11] + a[INDEX_SDIAN + 12] * da[INDEX_SDIAN + 12] + a[INDEX_DIAN + 11]
                * da[INDEX_DIAN + 11] + a[INDEX_DIAN + 12] * da[INDEX_DIAN + 12];

        // terms magnetic activity
        da[INDEX_AMG + 1] = this.p10 * coste * dkp;
        da[INDEX_AMG + 2] = this.p30 * coste * dkp;
        da[INDEX_AMG + 3] = this.p50 * coste * dkp;
        da[INDEX_AMG + 4] = this.p11 * this.ch * dkp;
        da[INDEX_AMG + 5] = this.p31 * this.ch * dkp;
        da[INDEX_AMG + 6] = this.p51 * this.ch * dkp;
        da[INDEX_AMG + 7] = this.p11 * this.sh * dkp;
        da[INDEX_AMG + 8] = this.p31 * this.sh * dkp;
        da[INDEX_AMG + 9] = this.p51 * this.sh * dkp;

        // function g(l) (additional periodic)
        fp += a[INDEX_AMG + 1] * da[INDEX_AMG + 1] + a[INDEX_AMG + 2] * da[INDEX_AMG + 2] + a[INDEX_AMG + 3]
                * da[INDEX_AMG + 3] + a[INDEX_AMG + 4] * da[INDEX_AMG + 4] + a[INDEX_AMG + 5] * da[INDEX_AMG + 5]
                + a[INDEX_AMG + 6] * da[INDEX_AMG + 6] + a[INDEX_AMG + 7] * da[INDEX_AMG + 7] + a[INDEX_AMG + 8]
                * da[INDEX_AMG + 8] + a[INDEX_AMG + 9] * da[INDEX_AMG + 9];

        dakp = (a[INDEX_AMG + 1] * this.p10 + a[INDEX_AMG + 2] * this.p30 + a[INDEX_AMG + 3] * this.p50) * coste
                + (a[INDEX_AMG + 4] * this.p11 + a[INDEX_AMG + 5] * this.p31 + a[INDEX_AMG + 6] * this.p51) * this.ch
                + (a[INDEX_AMG + 7] * this.p11 + a[INDEX_AMG + 7] * this.p31 + a[INDEX_AMG + 9] * this.p51) * this.sh;

        da[INDEX_KP + 5] = da[INDEX_KP + 5] + dakp * this.akp[2];
        da[INDEX_KP + 6] = da[INDEX_KP + 5] + dakp * c2fi * this.akp[2];

        // terms in longitude
        clfl = MathLib.cos(this.xlon);
        da[INDEX_LON + 1] = this.p11 * clfl;
        da[INDEX_LON + 2] = this.p21 * clfl;
        da[INDEX_LON + 3] = this.p31 * clfl;
        da[INDEX_LON + 4] = this.p41 * clfl;
        da[INDEX_LON + 5] = this.p51 * clfl;

        slfl = MathLib.sin(this.xlon);
        da[INDEX_LON + 6] = this.p11 * slfl;
        da[INDEX_LON + 7] = this.p21 * slfl;
        da[INDEX_LON + 8] = this.p31 * slfl;
        da[INDEX_LON + 9] = this.p41 * slfl;
        da[INDEX_LON + 10] = this.p51 * slfl;

        for (int i = 0; i < NB_LON; i++) {
            fp += a[INDEX_LON + i] * da[INDEX_LON + i];
        }

        // function g(l) sum (coupled with flux)
        return f0 + fp * f1f;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The following attributes are not deeply copied:
     * <ul>
     * <li>inputParams: {@link DTMInputParameters}</li>
     * <li>sun: {@link PVCoordinatesProvider}</li>
     * <li>earth: {@link BodyShape}</li>
     * </ul>
     * </p>
     */
    @Override
    public Atmosphere copy() {
        try {
            return new DTM2012(this.inputParams, this.sun, this.earth);
        } catch (final PatriusException e) {
            // It cannot happen
            throw new PatriusExceptionWrapper(e);
        }
    }
    // CHECKSTYLE: resume MagicNumber check
}
