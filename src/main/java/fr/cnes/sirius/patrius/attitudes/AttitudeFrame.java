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
 * @history creation 21/12/2011
 *
 * HISTORY
 * VERSION:4.9:DM:DM-3157:10/05/2022:[PATRIUS] Construction d'un AttitudeFrame a partir d'un AttitudeProvider 
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:178:06/01/2013:Corrected log id format
 * VERSION::FA:133:02/10/2013:Javadoc completed
 * VERSION::DM:524:25/05/2016:serialization
 * VERSION::DM:524:10/03/2016:serialization java doc
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.orbits.pvcoordinates.PVCoordinatesProvider;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * This class is a special implementation of the {@link Frame} class; it represents a dynamic spacecraft frame,
 * i.e. a dynamic frame whose orientation is defined by an attitude provider.
 * 
 * @serial AttitudeFrame is serializable given a serializable AttitudeProvider
 * 
 * @concurrency not thread safe
 * 
 * @concurrency.comment it is a Frame.
 * 
 * @author Julie Anton
 * 
 * @see AttitudeTransformProvider
 * 
 * @version $Id: AttitudeFrame.java 17582 2017-05-10 12:58:16Z bignon $
 * 
 * @since 1.1
 * 
 */
public class AttitudeFrame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 1239403019365033844L;

    /** SATELLITE_FRAME string. */
    private static final String SATELLITE_FRAME = "satellite frame";

    /** The attitude provider defining the orientation of the frame. */
    private final AttitudeProvider attProvider;

    /**
     * Constructor of the dynamic spacecraft frame.
     * 
     * @param pvProvider
     *        provides the position and velocity of the spacecraft given a date and a frame
     * @param attitudeProv
     *        the spacecraft attitude provider
     * @param referenceFrame
     *        the parent frame
     * @throws PatriusException
     *         if a problem occurs during frames transformations
     */
    public AttitudeFrame(final PVCoordinatesProvider pvProvider, final AttitudeProvider attitudeProv,
        final Frame referenceFrame) throws PatriusException {
        super(referenceFrame, new AttitudeTransformProvider(attitudeProv, pvProvider, referenceFrame), SATELLITE_FRAME);
        this.attProvider = attitudeProv;
    }

    /**
     * Gets the attitude provider defining the orientation of the frame.
     * 
     * @return the attitude provider defining the orientation of the frame.
     */
    public final AttitudeProvider getAttitudeProvider() {
        return this.attProvider;
    }
}
