/**
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
 * Copyright 2011-2012 Space Applications Services
 *
 * HISTORY
 * VERSION:4.12:DM:DM-62:17/08/2023:[PATRIUS] Création de l'interface BodyPoint
 * VERSION:4.11.1:FA:FA-61:30/06/2023:[PATRIUS] Code inutile dans la classe RediffusedFlux
 * VERSION:4.11:DM:DM-3295:22/05/2023:[PATRIUS] Conditions meteorologiques variables dans modeles troposphere
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.8:DM:DM-2929:15/11/2021:[PATRIUS] Harmonisation des modeles de troposphere 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:88:18/11/2013: refactoring and renaming of the class
 * VERSION::FA:400:17/03/2015: use class FastMath instead of class Math
 * VERSION::FA:1465:26/04/2018:multi-thread environment optimisation
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * VERSION::FA:1970:03/01/2019:quality corrections
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.signalpropagation.troposphere;

import java.util.ArrayList;

import fr.cnes.sirius.patrius.data.DataProvidersManager;
import fr.cnes.sirius.patrius.math.analysis.BivariateFunction;
import fr.cnes.sirius.patrius.math.analysis.interpolation.BicubicSplineInterpolator;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.math.util.MathLib;
import fr.cnes.sirius.patrius.models.earth.InterpolationTableLoader;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * A static tropospheric model that interpolates the actual tropospheric delay based on values read
 * from a configuration file (tropospheric-delay.txt) via the {@link DataProvidersManager}.
 * 
 * @author Thomas Neidhart
 */
public class FixedDelayModel implements TroposphericCorrection {

    /** Serializable UID. */
    private static final long serialVersionUID = -92320711761929077L;

    /** Half-circle (180deg). */
    private static final double HALF_CIRCLE = 180.;

    /** Singleton object for the default model. */
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private static volatile FixedDelayModel defaultModel;

    /** Abscissa grid for the bi-variate interpolation function read from the file. */
    private final double[] xArr;

    /** Ordinate grid for the bi-variate interpolation function read from the file. */
    private final double[] yArr;

    /** Values samples for the bi-variate interpolation function read from the file. */
    private final double[][] fArr;

    /** The height of the station in m above sea level [m]. */
    private final double height;

    /** Interpolation function for the tropospheric delays. */
    private final transient BivariateFunction delayFunction;

    /**
     * Creates a new {@link FixedDelayModel} instance.
     * 
     * @param xArrIn abscissa grid for the interpolation function
     * @param yArrIn ordinate grid for the interpolation function
     * @param fArrIn values samples for the interpolation function
     * @param heightIn the height of the station in m above sea level [m]
     */
    public FixedDelayModel(final double[] xArrIn, final double[] yArrIn, final double[][] fArrIn,
                           final double heightIn) {
        this.xArr = xArrIn.clone();
        this.yArr = yArrIn.clone();
        this.fArr = fArrIn.clone();
        this.delayFunction = new BicubicSplineInterpolator().interpolate(xArrIn, yArrIn, fArrIn);
        this.height = heightIn;
    }

    /**
     * Creates a new {@link FixedDelayModel} instance, and loads the delay values from the given
     * resource via the {@link DataProvidersManager}.
     * 
     * @param supportedName a regular expression for supported resource names
     * @param heightIn the height of the station in m above sea level [m]
     * @throws PatriusException if the resource could not be loaded
     */
    public FixedDelayModel(final String supportedName, final double heightIn)
        throws PatriusException {

        final InterpolationTableLoader loader = new InterpolationTableLoader();
        DataProvidersManager.getInstance().feed(supportedName, loader);

        if (loader.stillAcceptsData()) {
            throw new PatriusException(PatriusMessages.UNABLE_TO_FIND_RESOURCE, supportedName);
        }

        this.xArr = loader.getAbscissaGrid();
        this.yArr = loader.getOrdinateGrid();
        this.fArr = loader.getValuesSamples();
        this.delayFunction = new BicubicSplineInterpolator().interpolate(this.xArr, this.yArr,
            this.fArr);
        this.height = heightIn;
    }

    /** {@inheritDoc} */
    @Override
    public boolean supportsParameter(final Parameter param) {
        return false; // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public ArrayList<Parameter> getParameters() {
        return new ArrayList<>(); // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public double derivativeValue(final Parameter p, final double elevation) {
        return 0.; // No supported parameter yet
    }

    /** {@inheritDoc} */
    @Override
    public boolean isDifferentiableBy(final Parameter p) {
        return supportsParameter(p); // No supported parameter yet
    }

    /**
     * Returns the default model, loading delay values from the file "tropospheric-delay.txt".
     * 
     * @param height the height of the station in m above sea level [m]
     * @return the default model
     * @throws PatriusException if the file could not be loaded
     */
    @SuppressWarnings("PMD.NonThreadSafeSingleton")
    public static FixedDelayModel getDefaultModel(final double height) throws PatriusException {
        /** Use of Double-Check locking because of thrown exception */
        if (defaultModel == null) {
            synchronized (FixedDelayModel.class) {
                if (defaultModel == null) {
                    defaultModel = new FixedDelayModel("^tropospheric-delay\\.txt$", height);
                }
            }
        }
        return defaultModel;
    }

    /**
     * Calculates the tropospheric path delay for the signal path from a ground station to a
     * satellite.
     * 
     * @param elevation the elevation of the satellite [rad]
     * @return the path delay due to the troposphere [m]
     */
    public double computePathDelay(final double elevation) {
        // limit the height to 5000 m
        final double h = MathLib.min(MathLib.max(0, this.height), 5000);
        // limit the elevation to 0 - 180 degree
        final double ele = MathLib.min(180d, MathLib.max(0d, elevation));
        // mirror elevation at the right angle of 90 degree
        final double e = ele > HALF_CIRCLE / 2. ? HALF_CIRCLE - ele : ele;

        return this.delayFunction.value(h, e);
    }

    /**
     * Calculates the tropospheric path delay for the signal path from a ground station to a
     * satellite at a given date.
     * 
     * @param date
     *        date for meteo conditions
     * @param elevation
     *        the elevation of the satellite [rad]
     * @return the path delay due to the troposphere [m]
     */
    public double computePathDelay(final AbsoluteDate date, final double elevation) {
        return this.computePathDelay(elevation);
    }

    /** {@inheritDoc} */
    @Override
    public double computeSignalDelay(final AbsoluteDate date, final double elevation) {
        return this.computePathDelay(elevation) / Constants.SPEED_OF_LIGHT;
    }

    /**
     * Make sure the unserializable bivariate interpolation function is properly rebuilt.
     * 
     * @return replacement object, with bivariate function properly set up
     */
    private Object readResolve() {
        return new FixedDelayModel(this.xArr, this.yArr, this.fArr, this.height);
    }
}
