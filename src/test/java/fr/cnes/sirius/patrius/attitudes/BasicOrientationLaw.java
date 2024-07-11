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
 * @history creation 09/03/2012
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019:[PATRIUS et COLOSUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:1782:19/11/2018:generalisation of low-level math framework
 * END-HISTORY
 */

package fr.cnes.sirius.patrius.attitudes;

import fr.cnes.sirius.patrius.frames.Frame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * @description <p>
 *              Basic class implementing the IOrientationLaw interface and providing the same rotation at any date.
 *              </p>
 * 
 * @author Thomas Trapier
 * 
 * @version $Id: BasicOrientationLaw.java 17910 2017-09-11 11:58:16Z bignon $
 * 
 * @since 1.1
 */
public class BasicOrientationLaw implements IOrientationLaw {

    /** Serial UID. */
    private static final long serialVersionUID = 4890752232276099986L;

    /** the rotation */
    private final Rotation rotation;

    /** the frame to express the rotation */
    private final Frame frame;

    /**
     * Constructor
     * 
     * @param rot
     *        the rotation
     * @param expressionFrame
     *        the frame to express the rotation
     */
    public BasicOrientationLaw(final Rotation rot, final Frame expressionFrame) {
        this.rotation = rot;
        this.frame = expressionFrame;
    }

    @Override
    public Rotation getOrientation(final AbsoluteDate date, final Frame outputFrame) throws PatriusException {

        final Transform transform = this.frame.getTransformTo(outputFrame, date);

        final Vector3D newAxis = transform.transformVector(this.rotation.getAxis());

        return new Rotation(newAxis, this.rotation.getAngle());
    }

}
