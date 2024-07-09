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
 * @history creation 9/02/2012
 * 
 * HISTORY
* VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::FA:231:03/04/2014:bad updating of the assembly's tree of frames
 * VERSION::DM:319:27/04/2015:Rotation convention change in AngularCoordinates
 * VERSION::DM:570:05/04/2017:add PropulsiveProperty and TankProperty
 * VERSION::FA:1449:15/03/2018:part can have either a Tank or a Mass property, not both
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import fr.cnes.sirius.patrius.assembly.properties.PropulsiveProperty;
import fr.cnes.sirius.patrius.assembly.properties.TankProperty;
import fr.cnes.sirius.patrius.frames.UpdatableFrame;
import fr.cnes.sirius.patrius.frames.transformations.Transform;
import fr.cnes.sirius.patrius.frames.transformations.TransformStateProvider;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Rotation;
import fr.cnes.sirius.patrius.math.geometry.euclidean.threed.Vector3D;
import fr.cnes.sirius.patrius.propagation.SpacecraftState;
import fr.cnes.sirius.patrius.time.AbsoluteDate;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * <p>
 * - This class is the builder that shall be needed to create an instance of the assembly.
 * </p>
 * <p>
 * - Its purpose is to simplify the building process of the assembly. It provides the method that allow the user to add
 * a part to the assembly, and then to add properties to each of the parts.
 * </p>
 * 
 * @concurrency not thread-safe
 * 
 * @concurrency.comment The assembly is not thread-safe itself
 * 
 * @see Assembly
 * 
 * @author Thomas Trapier
 * 
 * @version $Id$
 * 
 * @since 1.1
 */
public class AssemblyBuilder {

    /**
     * The assembly instance
     */
    private Assembly assembly;

    /**
     * 
     * Simple constructor of an assembly builder.
     */
    public AssemblyBuilder() {
        this.assembly = new Assembly();
    }

    /**
     * Constructor with an assembly instance.
     * 
     * User should be aware that with this constructor, the reference to the assembly is shared with
     * the builder. It means that any change to it might have direct effect on current computation
     * using the assembly, until the returnAssembly() method is used to release the reference. This
     * functionality is designed to spare the user the building of a whole new assembly between two
     * computations, which can be a bit tedious when only small changes are needed.
     * 
     * @param inAssembly the assembly instance
     * 
     */
    public AssemblyBuilder(final Assembly inAssembly) {
        this.assembly = inAssembly;
    }

    /**
     * This method adds a new part to the currently built assembly.
     * The new part is defined relatively to its parent part with a transform law.
     * 
     * @param partName the name of the part
     * @param parentPartName the name of the parent of the part
     * @param transformStateProvider the transformation law that defines the new part's frame wrt the parent frame
     * @throws IllegalArgumentException if a part with this name already exists, if the main part
     *         has not been created yet or if the parent part does not exist (no part with this
     *         name).
     * 
     */
    public final void addPart(final String partName, final String parentPartName,
                              final TransformStateProvider transformStateProvider) {
        final IPart parentPart = assembly.getPart(parentPartName);
        final IPart newPart = new MobilePart(partName, parentPart, transformStateProvider);
        assembly.addPart(newPart);
    }

    /**
     * This method adds a new part to the currently built assembly, defining its new frame by a
     * Transform object.
     * 
     * @param partName the name of the part
     * @param parentPartName the name of the parent of the part
     * @param transform the transformation law that defines the new part's frame
     * @throws IllegalArgumentException if a part with this name already exists, if the main part
     *         has not been created yet or if the parent part does not exist (no part with this
     *         name).
     * 
     */
    public final void addPart(final String partName, final String parentPartName,
                              final Transform transform) {

        // search of the parent part
        final IPart parentPart = this.assembly.getPart(parentPartName);

        // creation of the new part
        final IPart newPart = new Part(partName, parentPart, transform);
        this.assembly.addPart(newPart);
    }

    /**
     * This method adds a new part to the currently built assembly. Its frame is defined by a
     * translation from its parent part's frame and then a rotation.
     * 
     * @param partName the name of the part
     * @param parentPartName the name of the parent of the part
     * @param translation the translation
     * @param rotation the rotation
     */
    public final void addPart(final String partName, final String parentPartName,
                              final Vector3D translation, final Rotation rotation) {

        // search of the parent part
        final IPart parentPart = this.assembly.getPart(parentPartName);
        final Transform transformRot = new Transform(AbsoluteDate.J2000_EPOCH, rotation);
        final Transform transformTrans = new Transform(AbsoluteDate.J2000_EPOCH, translation);
        final Transform transform = new Transform(AbsoluteDate.J2000_EPOCH, transformTrans,
            transformRot);

        // creation of the new part
        final IPart newPart = new Part(partName, parentPart, transform);
        this.assembly.addPart(newPart);
    }

    /**
     * This method returns the part whose name is given in parameter.
     * 
     * @param name the name of the part
     * @return the part
     */
    public final IPart getPart(final String name) {
        return this.assembly.getPart(name);
    }

    /**
     * This method removes one part from the assembly.
     * 
     * @param partName the name of the part to remove.
     */
    public final void removePart(final String partName) {
        this.assembly.removePart(partName);
    }

    /**
     * Adds the main part to the assembly : shall be done before adding any other part.
     * 
     * @param mainBodyName the name of the part
     */
    public final void addMainPart(final String mainBodyName) {
        final MainPart mainBodyPart = new MainPart(mainBodyName);
        this.assembly.addMainPart(mainBodyPart);
    }

    /**
     * Sets up the frame of the main part. Shall be used to link the assembly to the main tree of
     * frames.
     * <p>Warning: the assembly should contain no mobile parts, otherwise call 
     * {@link #initMainPartFrame(SpacecraftState)}</p>
     * 
     * @param newFrame the new frame of the main part
     * @throws PatriusException thrown if the assembly contains mobile parts
     */
    public final void initMainPartFrame(final UpdatableFrame newFrame) throws PatriusException {
        this.assembly.initMainPartFrame(newFrame);
    }

    /**
     * Sets up the main frame of the assembly from a "SpacecraftState" object. Shall be used to link
     * the assembly to the main tree of frames. The parent frame of the main part frame is the
     * SpacecraftState's frame.
     * 
     * @param state the new SpacecraftState of the assembly
     * @throws PatriusException if a problem occurs during frames transformations
     */
    public final void initMainPartFrame(final SpacecraftState state) throws PatriusException {
        this.assembly.initMainPartFrame(state);
    }

    /**
     * This method returns the assembly when the user wants to get the instance of the assembly that
     * has been built so far.
     * 
     * @return the assembly instance
     */
    public final Assembly returnAssembly() {
        // clear the reference to the assembly in the builder
        final Assembly assemblyRef = this.assembly;
        this.assembly = new Assembly();
        return assemblyRef;
    }

    /**
     * Adds a property of any type to a part of the assembly.
     * 
     * @precondition A part with this name exists and contains no property of this type.
     * 
     * @param property the property to add
     * @param partName the name of the part to which the property must be added
     * @throws IllegalArgumentException if the part already contains a property of this type or if
     *         the assembly contains no part of this name.
     */
    public final void addProperty(final IPartProperty property, final String partName) {
        // search of the part
        final IPart toBeDecorated = this.assembly.getPart(partName);

        // Specific handling of TankProperty/PropulsiveProperty: part name is necessary and is
        // provided by AssemblyBuilder instead of user to avoid user confusion between part name
        // provided to assembly builder and part name provided to property (which is the same)
        if (property.getType() == PropertyType.TANK) {
            ((TankProperty) property).setPartName(partName);
        }
        if (property.getType() == PropertyType.PROPULSIVE) {
            ((PropulsiveProperty) property).setPartName(partName);
        }

        // adding of the property
        toBeDecorated.addProperty(property);
    }
}
