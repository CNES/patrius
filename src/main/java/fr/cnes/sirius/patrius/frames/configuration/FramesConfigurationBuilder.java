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
 * @history creation 28/06/2012
  * VERSION::FA:648:27/07/2016:Corrected minor points staying from V3.2
  * VERSION::FA:1301:06/09/2017:Generalized EOP history
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.frames.configuration;

import fr.cnes.sirius.patrius.frames.configuration.eop.EOPHistory;

/**
 * <p>
 * Frame configuration builder utility, to assist the user in creating a {@link FramesConfiguration}. Two constructors
 * exist.
 * </p>
 * <li>The first one doesnt take any parameters and when called, creates an internal
 * {@link FramesConfigurationImplementation} that is initialized with no model, see
 * {@link fr.cnes.sirius.patrius.frames.configuration.tides.TidalCorrectionModelFactory#NO_TIDE NO_TIDE},
 * {@link fr.cnes.sirius.patrius.frames.configuration.libration.LibrationCorrectionModelFactory#NO_LIBRATION
 * NO_LIBRATION},
 * {@link fr.cnes.sirius.patrius.frames.configuration.precessionnutation.PrecessionNutationModelFactory#NO_PN NO_PN} and
 * {@link fr.cnes.sirius.patrius.frames.configuration.eop.NoEOP2000History}. <li>The second one takes a
 * {@link FramesConfiguration} as an input argument, and initializes the new internal {@link FramesConfiguration} with
 * the models from the user specified one.
 * 
 * @author Julie Anton, Rami Houdroge
 * @since 1.3
 * @version $Id: FramesConfigurationBuilder.java 18073 2017-10-02 16:48:07Z bignon $
 * 
 */
public final class FramesConfigurationBuilder {

    /** Current configuration. */
    private FramesConfigurationImplementation config = new FramesConfigurationImplementation();

    /**
     * Public default constructor.
     */
    public FramesConfigurationBuilder() {
        // Nothing to do
    }

    /**
     * Public constructor.
     * 
     * @param configIn
     *        user configuration
     */
    public FramesConfigurationBuilder(final FramesConfiguration configIn) {

        this.setDiurnalRotation(configIn.getDiurnalRotationModel());
        this.setPolarMotion(configIn.getPolarMotionModel());
        this.setPrecessionNutation(configIn.getPrecessionNutationModel());
        this.setEOPHistory(configIn.getEOPHistory());

    }

    /**
     * Setter for history provider in the builder.
     * 
     * @param history
     *        history provider
     */
    public void setEOPHistory(final EOPHistory history) {
        this.config.setEOPHistory(history);
    }

    /**
     * Setter for polar motion provider in the builder.
     * 
     * @param polarMotionModel
     *        polar motion provider
     */
    public void setPolarMotion(final PolarMotion polarMotionModel) {
        this.config.setPolarMotionModel(polarMotionModel);
    }

    /**
     * Setter for diurnal rotation provider in the builder.
     * 
     * @param diurnalRotationModel
     *        diurnal rotation provider
     */
    public void setDiurnalRotation(final DiurnalRotation diurnalRotationModel) {
        this.config.setDiurnalRotationModel(diurnalRotationModel);
    }

    /**
     * Setter for precession nutation provider in the builder.
     * 
     * @param precessionNutationModel
     *        precession nutation provider
     */
    public void setPrecessionNutation(final PrecessionNutation precessionNutationModel) {
        this.config.setPrecessionNutationModel(precessionNutationModel);
    }

    /**
     * Configuration builder using the current builder providers. Always returns a new instance.
     * The attribute {@link FramesConfigurationBuilder#config} is reinitialized here.
     * 
     * @return a new FramesConfiguration instance
     */
    public FramesConfiguration getConfiguration() {
        final FramesConfiguration result = this.config;
        this.config = new FramesConfigurationImplementation();
        return result;
    }

}
