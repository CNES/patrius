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
 */
/*
 *
 * HISTORY
* VERSION:4.13:DM:DM-132:08/12/2023:[PATRIUS] Suppression de la possibilite 
 *          de convertir les sorties de VacuumSignalPropagation 
* VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
* VERSION:4.9:DM:DM-3161:10/05/2022:[PATRIUS] Ajout d'une methode getNativeFrame() a l'interface PVCoordinatesProvider 
* VERSION:4.9:DM:DM-3165:10/05/2022:[PATRIUS] Amelioration de la propagation du signal 
* VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
* VERSION:4.8:DM:DM-3044:15/11/2021:[PATRIUS] Ameliorations du refactoring des sequences
* VERSION:4.3:DM:DM-2091:15/05/2019:[PATRIUS] optimisation du SpacecraftState
* VERSION:4.3:DM:DM-2099:15/05/2019:[PATRIUS] Possibilite de by-passer le critere du pas min dans l'integrateur numerique DOP853
* VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::DM:86:24/10/2013:New constructors
 * VERSION::FA:262:29/04/2014:Removed standard gravitational parameter from constructor
 * VERSION::DM:289:27/08/2014:Refactoring of SpacecraftState and harmonization of state vector
 * VERSION::FA:386:05/12/2014: index mutualisation for ephemeris interpolation
 * VERSION::FA:381:15/12/2014:Propagator tolerances and default mass issues
 * VERSION::FA:390:19/02/2015: added addAttitude method for AbstractEphemeris needs
 * VERSION::DM:290:04/03/2015: added toTransform methods
 * VERSION::DM:319:05/03/2015:Corrected Rotation class (Step1)
 * VERSION::DM:396:16/03/2015:new architecture for orbital parameters
 * VERSION::DM:368:20/03/2015:Eckstein-Heschler : Back at the "mu"
 * VERSION::DM:300:18/03/2015:Renamed AbstractAttitudeEquation into AttitudeEquation
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::FA:449:10/08/2015:Added error if attitudeForces == null and attitudeEvents != null
 * VERSION::DM:480:15/02/2016: new analytical propagators and mean/osculating conversion
 * VERSION::DM:489:17/12/2015:Refactoring of rotation acceleration by the AttitudeProvider
 * VERSION::FA:449:18/12/2015:Add coverage tests due to changes in attitude handling
 * VERSION::DM:654:04/08/2016:Add getAttitude(Frame) and getAttitude(LofType)
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.propagation;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fr.cnes.sirius.patrius.Utils;
import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.FramesFactory;
import fr.cnes.sirius.patrius.orbits.KeplerianOrbit;
import fr.cnes.sirius.patrius.orbits.Orbit;
import fr.cnes.sirius.patrius.orbits.PositionAngle;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.propagation.analytical.KeplerianPropagator;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.Constants;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PropagationException;

public class SpacecraftStateProviderTest {


    /**
     * @throws PatriusException if position cannot be computed in given frame
     * @testedMethod {@link SpacecraftStateProvider#getPVCoordinates(AbsoluteDate, Frame)}
     * 
     * @description the SpacecraftStateProvider interface should offer a default implementation of
     *              the {@link PVCoordinatesProvider#getPVCoordinates(AbsoluteDate, Frame)}. This
     *              test check the implementation behavior.
     * 
     * @testPassCriteria the PCCoordinates are returned as expected.
     */
    @Test
    public void testGetPVCoordinates() throws PatriusException {
        
        final Frame frame = FramesFactory.getGCRF();
        final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new KeplerianOrbit(7000000, 0.01, 0, 0, 0, 0, PositionAngle.TRUE,
                frame, date, Constants.GRIM5C1_EARTH_MU);
        final AbstractPropagator propagator = new KeplerianPropagator(orbit);
        final SpacecraftStateProvider scProvider = new SpacecraftStateProviderTemp(propagator);
        
        // Evaluate the SpacecraftStateProvider getPVCoordinates method behavior against the
        // AbstractPropagator getPVCoordinates method
        for (int i = 0; i < 100; i++) {
            final AbsoluteDate currentDate = date.shiftedBy(i);
            Assert.assertTrue(scProvider.getPVCoordinates(currentDate, frame).equals(
                    propagator.getPVCoordinates(currentDate, frame)));
        }
    }
    
    /**
     * Private class use to implement SpacecraftStateProvider without overriding the
     * getPVCoordinates method (to keep the default implementation in the interface).
     */
    private class SpacecraftStateProviderTemp implements SpacecraftStateProvider {

        /** Serializable UID. */
        private static final long serialVersionUID = 167607781812942741L;

        /** propagator. */
        private final AbstractPropagator propagator;
        
        /**
         * Basic constructor.
         * 
         * @param propagator
         *        Propagator
         */
        public SpacecraftStateProviderTemp(final AbstractPropagator propagator) {
            this.propagator = propagator;
        }

        /** {@inheritDoc} */
        @Override
        public SpacecraftState getSpacecraftState(final AbsoluteDate date)
                throws PropagationException {
            return this.propagator.propagate(date);
        }

        /** {@inheritDoc} */
        @Override
        public Frame getNativeFrame(final AbsoluteDate date) throws PatriusException {
            return this.propagator.getFrame();
        }
    }

    /**
     * Patrius data initialization and frames configuration.
     */
    @Before
    public void setUp() throws PatriusException {
        Utils.setDataRoot("regular-data");
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }

    /**
     * Frames configuration reset if changed.
     */
    @After
    public void tearDown() throws PatriusException {
        FramesFactory.setConfiguration(Utils.getIERS2003Configuration(true));
    }
}
