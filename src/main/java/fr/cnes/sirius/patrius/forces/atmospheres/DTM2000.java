/**
 * Copyright 2002-2012 CS Systèmes d'Information
 * Copyright 2011-2017 CNES
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
 */
/*
 * HISTORY
* VERSION:4.6:DM:DM-2528:27/01/2021:[PATRIUS] Integration du modele DTM 
 * VERSION:4.5:DM:DM-2445:27/05/2020:optimisation de SolarActivityReader 
 * VERSION:4.5:DM:DM-2369:27/05/2020:ajout d'elements dans l'interface de MathLib 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:156:24/10/2013:Removed GregorianCalendar dependency
 * VERSION::DM:268:30/04/2015:drag and lift implementation
 * VERSION::FA:576:22/03/2016:cache mechanism for density
 * VERSION::DM:606:05/08/2016:extended atmosphere data
 * VERSION::FA:1275:30/08/2017:correct partial density computation
 * VERSION::DM:1489:21/05/2018:add GENOPUS Custom classes
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.forces.atmospheres;

import fr.cnes.sirius.patrius.bodies.BodyShape;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusExceptionWrapper;

// CHECKSTYLE: stop MagicNumber check
// Reason: model - Orekit code

/**
 * This atmosphere model is the realization of the DTM-2000 model.
 * <p>
 * NB Since the initial implementation (rev 1665), the model has been modified to fix a bug (see Story V-83 for
 * validation) : the number of days in the year (cachedDay)is a double, instead of an integer. This modification should
 * be integrated in the OREKIT future version.
 * </p>
 * <p>
 * It is described in the paper: <br>
 * 
 * <b>The DTM-2000 empirical thermosphere model with new data assimilation and constraints at lower boundary: accuracy
 * and properties</b><br>
 * 
 * <i>S. Bruinsma, G. Thuillier and F. Barlier</i> <br>
 * 
 * Journal of Atmospheric and Solar-Terrestrial Physics 65 (2003) 1053–1070<br>
 * 
 * </p>
 * <p>
 * Two computation methods are proposed to the user:
 * <ul>
 * <li>one PATRIUS independent and compliant with initial FORTRAN routine entry values:
 * {@link #getDensity(double, double, double, double, double, double, double, double, double)}.</li>
 * <li>one compliant with OREKIT Atmosphere interface, necessary to the
 * {@link fr.cnes.sirius.patrius.forces.drag.DragForce drag force model} computation.</li>
 * </ul>
 * </p>
 * <p>
 * This model provides dense output for altitudes beyond 120 km. Computed data are:
 * <ul>
 * <li>Temperature at altitude z (K)</li>
 * <li>Exospheric temperature above input position (K)</li>
 * <li>Vertical gradient of T a 120 km</li>
 * <li>Total density (kg/m<sup>3</sup>)</li>
 * <li>Mean atomic mass</li>
 * <li>Partial densities in (kg/m<sup>3</sup>) : hydrogen, helium, atomic oxygen, molecular nitrogen, molecular oxygen,
 * atomic nitrogen</li>
 * </ul>
 * </p>
 * <p>
 * The model needs geographical and time information to compute general values, but also needs space weather data : mean
 * and instantaneous solar flux and geomagnetic indices.
 * </p>
 * <p>
 * Mean solar flux is (for the moment) represented by the F10.7 indices. Instantaneous flux can be set to the mean value
 * if the data is not available. Geomagnetic activity is represented by the Kp indice, which goes from 1 (very low
 * activity) to 9 (high activity).
 * </p>
 * </p>
 * All these data can be found on the <a href="http://sec.noaa.gov/Data/index.html"> NOAA (National
 * Oceanic and Atmospheric Administration) website.</a> </p>
 * 
 * <p>
 * Mod : Modified line 871, added TimeZone.getTimeZone("GMT+00:00") to get a GregorianCalendar that doesnt depend on the
 * machine locale time zone.
 * </p>
 * <p>
 * Mod : DTM2000 thread-safety improved : instances in different threads no longer corrupt each others' computations.
 * But sharing one instance between several threads is untested (no realistic use case found).
 * </p>
 * 
 * @author R. Biancale, S. Bruinsma: original fortran routine
 * @author Fabien Maussion (java translation)
 */

@SuppressWarnings("PMD.NullAssignment")
public class DTM2000 extends AbstractDTM {
	
    /** Resources text file. */
    private static final String DATA_FILE_DTM2000 = "/META-INF/dtm_2000.txt";
    
    
    /**
     * Simple constructor for independent computation.
     * 
     * @param parameters the solar and magnetic activity data
     * @param sunIn the sun position
     * @param earthIn the earth body shape
     * @exception PatriusException if some resource file reading error occurs
     */
    public DTM2000(final DTMInputParameters parameters, final PVCoordinatesProvider sunIn,
                   final BodyShape earthIn) throws PatriusException {
    	super(parameters, sunIn, earthIn, DATA_FILE_DTM2000);
        // initialize partial derivatives arrays
        this.initderivatives();
    }
   
    /**
     * {@inheritDoc}
     */
    @Override
    protected void densityComputationFromFortran(){
        // Initialization
        this.ro = 0.0;

        // + dzlb ??
        final double zlb = ZLB0;
        
        //compute geographic and magnetic pole coefficients with Legendre polynomials
        computeGeographicMagneticCoefficients();
        
        // compute function g(l) / tinf, t120, tp120
        int kleq = 1;
        final double gdelt = this.gFunction(tt, this.dtt, 1, kleq);
        this.dtt[1] = 1.0 + gdelt;
        this.tinf = tt[1] * this.dtt[1];

        // equinox
        kleq = 0;

        if ((this.cachedDay < 59.) || (this.cachedDay > 284.)) {
            // north winter
            kleq = -1;
        }
        if ((this.cachedDay > 99.) && (this.cachedDay < 244.)) {
            // north summer
            kleq = 1;
        }

        final double gdelt0 = this.gFunction(t0, this.dt0, 0, kleq);
        this.dt0[1] = (t0[1] + gdelt0) / t0[1];
        final double t120 = t0[1] + gdelt0;
        final double gdeltp = this.gFunction(tp, this.dtp, 0, kleq);
        this.dtp[1] = (tp[1] + gdeltp) / tp[1];
        final double tp120 = tp[1] + gdeltp;

        // compute n(z) concentrations: H, He, O, N2, O2, N
        final double sigma = tp120 / (this.tinf - t120);
        final double dzeta = (RE + zlb) / (RE + this.cachedAlti);
        final double zeta = (this.cachedAlti - zlb) * dzeta;
        final double sigzeta = sigma * zeta;
        final double expsz = MathLib.exp(-sigzeta);
        this.cachedTemperature = this.tinf - (this.tinf - t120) * expsz;

        final double[] dbase = new double[7];

        kleq = 1;

        final double gdelh = this.gFunction(h, this.dh, 0, kleq);
        this.dh[1] = MathLib.exp(gdelh);
        dbase[1] = h[1] * this.dh[1];

        final double gdelhe = this.gFunction(he, this.dhe, 0, kleq);
        this.dhe[1] = MathLib.exp(gdelhe);
        dbase[2] = he[1] * this.dhe[1];

        final double gdelo = this.gFunction(o, this.dox, 1, kleq);
        this.dox[1] = MathLib.exp(gdelo);
        dbase[3] = o[1] * this.dox[1];

        final double gdelaz2 = this.gFunction(az2, this.daz2, 1, kleq);
        this.daz2[1] = MathLib.exp(gdelaz2);
        dbase[4] = az2[1] * this.daz2[1];

        final double gdelo2 = this.gFunction(o2, this.do2, 1, kleq);
        this.do2[1] = MathLib.exp(gdelo2);
        dbase[5] = o2[1] * this.do2[1];

        final double gdelaz = this.gFunction(az, this.daz, 1, kleq);
        this.daz[1] = MathLib.exp(gdelaz);
        dbase[6] = az[1] * this.daz[1];

        final double zlbre = 1.0 + zlb / RE;
        final double glb = (GSURF / (zlbre * zlbre)) / (sigma * RGAS * this.tinf);
        final double t120tz = t120 / this.cachedTemperature;

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
        // No result to return
        // Class variables modified directly
    }
    
    /**
     * {@inheritDoc}
     */
    // CHECKSTYLE: stop MethodLength check
    // CHECKSTYLE: stop CommentRatio check
    // Reason: Orekit code kept as such
    @Override
    protected double gFunction(final double[] a, final double[] da, final int ff0, final int kleEq) {
        // CHECKSTYLE: resume MethodLength check
        // CHECKSTYLE: resume CommentRatio check

        final double[] fmfb = new double[3];
        final double[] fbm150 = new double[3];

        // latitude terms
        da[2] = this.p20;
        da[3] = this.p40;
        da[74] = this.p10;
        double a74 = a[74];
        double a77 = a[77];
        double a78 = a[78];
        if (kleEq == -1) {
            // winter
            a74 = -a74;
            a77 = -a77;
            a78 = -a78;
        }
        if (kleEq == 0) {
            // equinox
            a74 = this.semestrialCorrection(a74);
            a77 = this.semestrialCorrection(a77);
            a78 = this.semestrialCorrection(a78);
        }
        da[77] = this.p30;
        da[78] = this.p50;
        da[79] = this.p60;

        // flux terms
        fmfb[1] = this.cachedF[1] - this.cachedFbar[1];
        fmfb[2] = this.cachedF[2] - this.cachedFbar[2];
        fbm150[1] = this.cachedFbar[1] - 150.0;
        fbm150[2] = this.cachedFbar[2];
        da[4] = fmfb[1];
        da[6] = fbm150[1];
        da[4] = da[4] + a[70] * fmfb[2];
        da[6] = da[6] + a[71] * fbm150[2];
        da[70] = fmfb[2] * (a[4] + 2.0 * a[5] * da[4] + a[82] * this.p10 + a[83] * this.p20 + a[84] * this.p30);
        da[71] = fbm150[2] * (a[6] + 2.0 * a[69] * da[6] + a[85] * this.p10 + a[86] * this.p20 + a[87] * this.p30);
        da[5] = da[4] * da[4];
        da[69] = da[6] * da[6];
        da[82] = da[4] * this.p10;
        da[83] = da[4] * this.p20;
        da[84] = da[4] * this.p30;
        da[85] = da[6] * this.p20;
        da[86] = da[6] * this.p30;
        da[87] = da[6] * this.p40;

        // Kp terms
        final int ikp = 62;
        final int ikpm = 67;
        final double c2fi = 1.0 - this.p10mg * this.p10mg;
        final double dkp = this.akp[1] + (a[ikp] + c2fi * a[ikp + 1]) * this.akp[2];
        double dakp =
            a[7] + a[8] * this.p20mg + a[68] * this.p40mg + 2.0 * dkp
                    * (a[60] + a[61] * this.p20mg + a[75] * 2.0 * dkp * dkp);
        da[ikp] = dakp * this.akp[2];
        da[ikp + 1] = da[ikp] * c2fi;
        final double dkpm = this.akp[3] + a[ikpm] * this.akp[4];
        final double dakpm =
            a[64] + a[65] * this.p20mg + a[72] * this.p40mg + 2.0 * dkpm
                    * (a[66] + a[73] * this.p20mg + a[76] * 2.0 * dkpm * dkpm);
        da[ikpm] = dakpm * this.akp[4];
        da[7] = dkp;
        da[8] = this.p20mg * dkp;
        da[68] = this.p40mg * dkp;
        da[60] = dkp * dkp;
        da[61] = this.p20mg * da[60];
        da[75] = da[60] * da[60];
        da[64] = dkpm;
        da[65] = this.p20mg * dkpm;
        da[72] = this.p40mg * dkpm;
        da[66] = dkpm * dkpm;
        da[73] = this.p20mg * da[66];
        da[76] = da[66] * da[66];

        // non-periodic g(l) function
        double f0 =
            a[4] * da[4] + a[5] * da[5] + a[6] * da[6] + a[69] * da[69] + a[82] * da[82]
                    + a[83] * da[83] + a[84] * da[84] + a[85] * da[85] + a[86] * da[86] + a[87]
                    * da[87];
        final double f1f = 1.0 + f0 * ff0;

        f0 =
            f0 + a[2] * da[2] + a[3] * da[3] + a74 * da[74] + a77 * da[77] + a[7] * da[7]
                    + a[8] * da[8] + a[60] * da[60] + a[61] * da[61] + a[68] * da[68] + a[64]
                    * da[64] + a[65] * da[65] + a[66] * da[66] + a[72] * da[72] + a[73]
                    * da[73] + a[75] * da[75] + a[76] * da[76] + a78 * da[78] + a[79] * da[79];
        // termes annuels symetriques en latitude
        da[9] = MathLib.cos(ROT * (this.cachedDay - a[11]));
        da[10] = this.p20 * da[9];
        // termes semi-annuels symetriques en latitude
        da[12] = MathLib.cos(ROT2 * (this.cachedDay - a[14]));
        da[13] = this.p20 * da[12];
        // termes annuels non symetriques en latitude
        final double coste = MathLib.cos(ROT * (this.cachedDay - a[18]));
        da[15] = this.p10 * coste;
        da[16] = this.p30 * coste;
        da[17] = this.p50 * coste;
        // terme semi-annuel non symetrique en latitude
        final double cos2te = MathLib.cos(ROT2 * (this.cachedDay - a[20]));
        da[19] = this.p10 * cos2te;
        da[39] = this.p30 * cos2te;
        da[59] = this.p50 * cos2te;
        // termes diurnes [et couples annuel]
        da[21] = this.p11 * this.ch;
        da[22] = this.p31 * this.ch;
        da[23] = this.p51 * this.ch;
        da[24] = da[21] * coste;
        da[25] = this.p21 * this.ch * coste;
        da[26] = this.p11 * this.sh;
        da[27] = this.p31 * this.sh;
        da[28] = this.p51 * this.sh;
        da[29] = da[26] * coste;
        da[30] = this.p21 * this.sh * coste;
        // termes semi-diurnes [et couples annuel]
        da[31] = this.p22 * this.c2h;
        da[37] = this.p42 * this.c2h;
        da[32] = this.p32 * this.c2h * coste;
        da[33] = this.p22 * this.s2h;
        da[38] = this.p42 * this.s2h;
        da[34] = this.p32 * this.s2h * coste;
        da[88] = this.p32 * this.c2h;
        da[89] = this.p32 * this.s2h;
        da[90] = this.p52 * this.c2h;
        da[91] = this.p52 * this.s2h;
        double a88 = a[88];
        double a89 = a[89];
        double a90 = a[90];
        double a91 = a[91];
        if (kleEq == -1) {
            // hiver
            a88 = -a88;
            a89 = -a89;
            a90 = -a90;
            a91 = -a91;
        }
        if (kleEq == 0) {
            // equinox
            a88 = this.semestrialCorrection(a88);
            a89 = this.semestrialCorrection(a89);
            a90 = this.semestrialCorrection(a90);
            a91 = this.semestrialCorrection(a91);
        }
        da[92] = this.p62 * this.c2h;
        da[93] = this.p62 * this.s2h;
        // termes ter-diurnes
        da[35] = this.p33 * this.c3h;
        da[36] = this.p33 * this.s3h;
        // fonction g[l] periodique
        double fp =
            a[9] * da[9] + a[10] * da[10] + a[12] * da[12] + a[13] * da[13] + a[15] * da[15]
                    + a[16] * da[16] + a[17] * da[17] + a[19] * da[19] + a[21] * da[21]
                    + a[22] * da[22] + a[23] * da[23] + a[24] * da[24] + a[25] * da[25]
                    + a[26] * da[26] + a[27] * da[27] + a[28] * da[28] + a[29] * da[29]
                    + a[30] * da[30] + a[31] * da[31] + a[32] * da[32] + a[33] * da[33]
                    + a[34] * da[34] + a[35] * da[35] + a[36] * da[36] + a[37] * da[37] + a[38]
                    * da[38] + a[39] * da[39] + a[59] * da[59] + a88 * da[88] + a89 * da[89]
                    + a90 * da[90] + a91 * da[91] + a[92] * da[92] + a[93] * da[93];

        // termes d'activite magnetique
        da[40] = this.p10 * coste * dkp;
        da[41] = this.p30 * coste * dkp;
        da[42] = this.p50 * coste * dkp;
        da[43] = this.p11 * this.ch * dkp;
        da[44] = this.p31 * this.ch * dkp;
        da[45] = this.p51 * this.ch * dkp;
        da[46] = this.p11 * this.sh * dkp;
        da[47] = this.p31 * this.sh * dkp;
        da[48] = this.p51 * this.sh * dkp;

        // fonction g[l] periodique supplementaire
        fp +=
            a[40] * da[40] + a[41] * da[41] + a[42] * da[42] + a[43] * da[43] + a[44] * da[44]
                    + a[45] * da[45] + a[46] * da[46] + a[47] * da[47] + a[48] * da[48];

        dakp =
            (a[40] * this.p10 + a[41] * this.p30 + a[42] * this.p50) * coste
                    + (a[43] * this.p11 + a[44] * this.p31 + a[45] * this.p51) * this.ch
                    + (a[46] * this.p11 + a[47] * this.p31 + a[48] * this.p51) * this.sh;
        da[ikp] += dakp * this.akp[2];
        da[ikp + 1] = da[ikp] + dakp * c2fi * this.akp[2];
        // termes de longitude
        final double clfl = MathLib.cos(this.xlon);
        da[49] = this.p11 * clfl;
        da[50] = this.p21 * clfl;
        da[51] = this.p31 * clfl;
        da[52] = this.p41 * clfl;
        da[53] = this.p51 * clfl;
        final double slfl = MathLib.sin(this.xlon);
        da[54] = this.p11 * slfl;
        da[55] = this.p21 * slfl;
        da[56] = this.p31 * slfl;
        da[57] = this.p41 * slfl;
        da[58] = this.p51 * slfl;

        // fonction g[l] periodique supplementaire
        fp +=
            a[49] * da[49] + a[50] * da[50] + a[51] * da[51] + a[52] * da[52] + a[53] * da[53]
                    + a[54] * da[54] + a[55] * da[55] + a[56] * da[56] + a[57] * da[57] + a[58]
                    * da[58];

        // fonction g(l) totale (couplage avec le flux)
        return f0 + fp * f1f;
    }
    
    
    /**
     * Apply a correction coefficient to the given parameter.
     * 
     * @param param the parameter to correct
     * @return the corrected parameter
     */
    private double semestrialCorrection(final double param) {
        final int debeqpr = 59;
        final int debeqau = 244;
        final double xmult;
        final double result;
        if (this.cachedDay >= 100.0) {
            // Compute coefficient and result
            xmult = (this.cachedDay - debeqau) / 40.0;
            result = param - 2.0 * param * xmult;
        } else {
            // Compute coefficient and result
            xmult = (this.cachedDay - debeqpr) / 40.0;
            result = 2.0 * param * xmult - param;
        }
        // Return corrected parameter
        return result;
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
            return new DTM2000(this.inputParams, this.sun, this.earth);
        } catch (final PatriusException e) {
            // It cannot happen
            throw new PatriusExceptionWrapper(e);
        }
    }
    // CHECKSTYLE: resume MagicNumber check
}
