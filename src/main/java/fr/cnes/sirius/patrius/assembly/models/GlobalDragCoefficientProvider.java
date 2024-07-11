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
 * @history creation 20/03/2017
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2089:15/05/2019:[PATRIUS] passage a Java 8
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:849:20/03/2017:Implementation of DragCoefficientProvider with file reader
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly.models;

import java.io.IOException;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.forces.atmospheres.AtmosphereData;
import fr.cnes.sirius.patrius.math.analysis.TrivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TriLinearIntervalsInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TricubicSplineInterpolator;
import fr.cnes.sirius.patrius.math.analysis.interpolation.TrivariateGridInterpolator;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Implementation of {@link DragCoefficientProvider} computing aero coefficients
 * as functions of type {@link TrivariateFunction}.
 * The service {@link DragCoefficientProvider #getCoefficients(Vector3D, AtmosphereData, Assembly)} is
 * implemented using these functions in which a 3D interpolation (linear or by spline) is performed.
 * The considered interpolation point has (azimuth, elevation, s) coordinates, s being the velocity factor,
 * such that :
 * <ul>
 * <li>the azimuth is the angle between X satellite axis and the projection of the relative velocity in satellite plane
 * (XY).</li>
 * <li>the elevation is the angle between the relative velocity and its projection in satellite plane (XY), in the
 * satellite's frame.</li>
 * <li>s is computed knowing :
 * 
 * <pre>
 * s<sup>2</sup>  = || V_rel ||<sup>2</sup> / 2r T_atmos
 * </pre>
 * 
 * </li>
 * </ul>
 * 
 * This model uses 3 parameters (azimuth, elevation, s factor) to compute 12 output being the aero coefficients : <li>
 * SC_abs (x, y, z): absorption coefficients <li>SC_spec (x, y, z): specular coefficients <li>SC_diff_av (x, y, z):
 * diffuse coefficients (front face) <li>SC_diff_ar (x, y, z): diffuse coefficients (rear face)
 * 
 * @concurrency thread-safe
 * 
 * @see InterpolatedDragReader
 * 
 * @author rodriguest
 * 
 * @version $Id$
 * 
 * @since 3.4
 * 
 */
public class GlobalDragCoefficientProvider implements DragCoefficientProvider {

    /** Serial UID. */
    private static final long serialVersionUID = -2219197514035528774L;

    /** Two Pi (deg). */
    private static final double TWO_PI_DEG = 360.0;

    /** SCXABS_INDEX index. */
    private static final int SCXABS_INDEX = 3;

    /** SCYABS_INDEX index. */
    private static final int SCYABS_INDEX = 4;

    /** SCZABS_INDEX index. */
    private static final int SCZABS_INDEX = 5;

    /** SCXSPEC_INDEX index. */
    private static final int SCXSPEC_INDEX = 6;

    /** SCYSPEC_INDEX index. */
    private static final int SCYSPEC_INDEX = 7;

    /** SCZSPEC_INDEX index. */
    private static final int SCZSPEC_INDEX = 8;

    /** SCXDIFFAV index. */
    private static final int SCXDIFFAV_INDEX = 9;

    /** SCYDIFFAV index. */
    private static final int SCYDIFFAV_INDEX = 10;

    /** SCZDIFFAV index. */
    private static final int SCZDIFFAV_INDEX = 11;

    /** SCXDIFFAR index. */
    private static final int SCXDIFFAR_INDEX = 12;

    /** SCYDIFFAR index. */
    private static final int SCYDIFFAR_INDEX = 13;

    /** SCZDIFFAR index. */
    private static final int SCZDIFFAR_INDEX = 14;

    /** Number of input. */
    private static final int N_INPUT = 3;

    /** Number of output. */
    private static final int N_OUTPUT = 12;

    /** Enumerate to choose the way to interpolate. */
    public enum INTERP {
        /** Linear interpolation. */
        LINEAR,
        /** Interpolation by splines. */
        SPLINE;
    }

    /** SC coefficient due to absorption on X satellite axis. */
    private final TrivariateFunction fscxAbs;
    /** SC coefficient due to absorption on Y satellite axis. */
    private final TrivariateFunction fscyAbs;
    /** SC coefficient due to absorption on Z satellite axis. */
    private final TrivariateFunction fsczAbs;
    /** SC coefficient due to specular reemission on X satellite axis. */
    private final TrivariateFunction fscxSpec;
    /** SC coefficient due to specular reemission on Y satellite axis. */
    private final TrivariateFunction fscySpec;
    /** SC coefficient due to specular reemission on Z satellite axis. */
    private final TrivariateFunction fsczSpec;
    /** SCX coefficient due to diffused reemission (front) on X satellite axis. */
    private final TrivariateFunction fscxDiffAv;
    /** SCX coefficient due to diffused reemission (front) on Y satellite axis. */
    private final TrivariateFunction fscyDiffAv;
    /** SCX coefficient due to diffused reemission (front) on Z satellite axis. */
    private final TrivariateFunction fsczDiffAv;
    /** SCX coefficient due to diffused reemission (rear) on X satellite axis. */
    private final TrivariateFunction fscxDiffAr;
    /** SCX coefficient due to diffused reemission (rear) on Y satellite axis. */
    private final TrivariateFunction fscyDiffAr;
    /** SCX coefficient due to diffused reemission (rear) on Z satellite axis. */
    private final TrivariateFunction fsczDiffAr;

    /**
     * Constructor.
     * 
     * @param method
     *        the method chosen for interpolation : could be linear or by splines
     * @param filePath
     *        absolute path to the file containing the aero coefficients
     * @throws IOException
     *         if file could not be read.
     * @throws PatriusException
     *         thrown if number of input + output in file is not 15 or some lines are missing
     */
    // CHECKSTYLE: stop MethodLength check
    public GlobalDragCoefficientProvider(final INTERP method,
        final String filePath) throws IOException, PatriusException {
        // CHECKSTYLE: resume MethodLength check

        // Create the reader to read aero coefficients file and get data from it
        final InterpolatedDragReader aeroReader = new InterpolatedDragReader();
        final double[][] aeroData = aeroReader.readFile(filePath);
        final int size = aeroData.length;

        this.checkConsistency(aeroData);

        // Get azimuths, elevations and s factors possible values
        final SortedSet<Double> az = new TreeSet<Double>();
        final SortedSet<Double> elev = new TreeSet<Double>();
        final SortedSet<Double> sFact = new TreeSet<Double>();

        // Loop on data
        for (int i = 0; i < size; i++) {
            elev.add(aeroData[i][0]);
            az.add(aeroData[i][1]);
            sFact.add(aeroData[i][2]);
        }

        // Build azimuths, elevations and s factors arrays
        final int sizeEl = elev.size();
        final int sizeAz = az.size();
        final int sizeSFact = sFact.size();

        // Copy sets on arrays
        final Iterator<Double> iterEl = elev.iterator();
        final Iterator<Double> iterAz = az.iterator();
        final Iterator<Double> iterSfact = sFact.iterator();
        final double[] elevations = new double[sizeEl];
        final double[] azimuths = new double[sizeAz];
        final double[] sfactors = new double[sizeSFact];
        int count1 = 0;
        int count2 = 0;
        int count3 = 0;
        while (iterEl.hasNext()) {
            elevations[count1] = iterEl.next();
            count1++;
        }
        while (iterAz.hasNext()) {
            azimuths[count2] = iterAz.next();
            count2++;
        }
        while (iterSfact.hasNext()) {
            sfactors[count3] = iterSfact.next();
            count3++;
        }

        // Build SC as 3D arrays
        final double[][][] scxAbs = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] scyAbs = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] sczAbs = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] scxSpec = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] scySpec = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] sczSpec = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] scxDiffAv = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] scyDiffAv = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] sczDiffAv = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] scxDiffAr = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] scyDiffAr = new double[sizeEl][sizeAz][sizeSFact];
        final double[][][] sczDiffAr = new double[sizeEl][sizeAz][sizeSFact];

        // Fill arrays
        try {
            int line = 0;
            for (int i = 0; i < sizeEl; i++) {
                for (int j = 0; j < sizeAz; j++) {
                    for (int k = 0; k < sizeSFact; k++) {
                        scxAbs[i][j][k] = aeroData[line][SCXABS_INDEX];
                        scyAbs[i][j][k] = aeroData[line][SCYABS_INDEX];
                        sczAbs[i][j][k] = aeroData[line][SCZABS_INDEX];
                        scxSpec[i][j][k] = aeroData[line][SCXSPEC_INDEX];
                        scySpec[i][j][k] = aeroData[line][SCYSPEC_INDEX];
                        sczSpec[i][j][k] = aeroData[line][SCZSPEC_INDEX];
                        scxDiffAv[i][j][k] = aeroData[line][SCXDIFFAV_INDEX];
                        scyDiffAv[i][j][k] = aeroData[line][SCYDIFFAV_INDEX];
                        sczDiffAv[i][j][k] = aeroData[line][SCZDIFFAV_INDEX];
                        scxDiffAr[i][j][k] = aeroData[line][SCXDIFFAR_INDEX];
                        scyDiffAr[i][j][k] = aeroData[line][SCYDIFFAR_INDEX];
                        sczDiffAr[i][j][k] = aeroData[line][SCZDIFFAR_INDEX];
                        line++;
                    }
                }
            }
        } catch (final ArrayIndexOutOfBoundsException e) {
            // Some lines are missing
            throw new PatriusException(PatriusMessages.PDB_MISSING_LINES, e);
        }

        // Define interpolator
        final TrivariateGridInterpolator interpolator = (method == INTERP.LINEAR) ?
            new TriLinearIntervalsInterpolator() : new TricubicSplineInterpolator();

        // Build Trivariate functions using interpolation
        this.fscxAbs = interpolator.interpolate(elevations, azimuths, sfactors, scxAbs);
        this.fscyAbs = interpolator.interpolate(elevations, azimuths, sfactors, scyAbs);
        this.fsczAbs = interpolator.interpolate(elevations, azimuths, sfactors, sczAbs);
        this.fscxSpec = interpolator.interpolate(elevations, azimuths, sfactors, scxSpec);
        this.fscySpec = interpolator.interpolate(elevations, azimuths, sfactors, scySpec);
        this.fsczSpec = interpolator.interpolate(elevations, azimuths, sfactors, sczSpec);
        this.fscxDiffAv = interpolator.interpolate(elevations, azimuths, sfactors, scxDiffAv);
        this.fscyDiffAv = interpolator.interpolate(elevations, azimuths, sfactors, scyDiffAv);
        this.fsczDiffAv = interpolator.interpolate(elevations, azimuths, sfactors, sczDiffAv);
        this.fscxDiffAr = interpolator.interpolate(elevations, azimuths, sfactors, scxDiffAr);
        this.fscyDiffAr = interpolator.interpolate(elevations, azimuths, sfactors, scyDiffAr);
        this.fsczDiffAr = interpolator.interpolate(elevations, azimuths, sfactors, sczDiffAr);
    }

    /**
     * Check file consistency.
     * 
     * @param aeroData
     *        file data
     * @throws PatriusException
     *         thrown if input file does not have 15 (3 + 12) columns
     */
    private void checkConsistency(final double[][] aeroData) throws PatriusException {
        // Check there is 3 (input) + 12 (output) in file
        if (aeroData[0].length != N_INPUT + N_OUTPUT) {
            throw new PatriusException(PatriusMessages.PDB_WRONG_COLUMNS_NUMBER, aeroData[0].length, N_INPUT, N_OUTPUT);
        }
    }

    @Override
    /** {@inheritDoc} */
    public DragCoefficient getCoefficients(final Vector3D relativeVelocity, final AtmosphereData atmoData,
                                           final Assembly assembly) {

        // Use the opposite velocity given in output since the convention in DragSensitive
        // (relative velocity of atmosphere with respect to spacecraft) since it is not the
        // same as data convention (relative velocity of spacecraft with respect to atmosphere)
        final Vector3D relativeVelData = relativeVelocity.negate();

        // Compute elevation in [-90°, 90°]
        final double elevation = MathLib.toDegrees(relativeVelData.getDelta());

        // Compute azimuth in [0°, 360°]
        double azimuth = MathLib.toDegrees(relativeVelData.getAlpha());
        azimuth = (azimuth < 0) ? TWO_PI_DEG + azimuth : azimuth;

        // Compute s factor
        final double molarMass = atmoData.getMeanAtomicMass() * Constants.AVOGADRO_CONSTANT
            * AtmosphereData.HYDROGEN_MASS;
        final double tAtmo = atmoData.getLocalTemperature();
        final double vrelnorm2 = relativeVelData.getNormSq();
        final double r = MathLib.divide(Constants.PERFECT_GAS_CONSTANT, molarMass);
        final double s = MathLib.sqrt(vrelnorm2 / (2. * r * tAtmo));

        // Interpolate in SC functions at point (azimuth, elevation, sFact) to compute the drag coefficient
        return new DragCoefficient(
            new Vector3D(
                this.fscxAbs.value(elevation, azimuth, s),
                this.fscyAbs.value(elevation, azimuth, s),
                this.fsczAbs.value(elevation, azimuth, s)),
            new Vector3D(
                this.fscxSpec.value(elevation, azimuth, s),
                this.fscySpec.value(elevation, azimuth, s),
                this.fsczSpec.value(elevation, azimuth, s)),
            new Vector3D(
                this.fscxDiffAv.value(elevation, azimuth, s),
                this.fscyDiffAv.value(elevation, azimuth, s),
                this.fsczDiffAv.value(elevation, azimuth, s)),
            new Vector3D(
                this.fscxDiffAr.value(elevation, azimuth, s),
                this.fscyDiffAr.value(elevation, azimuth, s),
                this.fsczDiffAr.value(elevation, azimuth, s)));
    }

}
