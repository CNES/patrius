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
 * @history Created 25/02/2013
 *
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * VERSION::DM:284:06/10/2014:New architecture for parameterizable Parameters
 * VERSION::DM:200:28/08/2014: dealing with a negative mass in the propagator
 * VERSION::FA:358:09/03/2015:proper handling of vehicle negative surface
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.stela;

import fr.cnes.sirius.patrius.assembly.Assembly;
import fr.cnes.sirius.patrius.assembly.AssemblyBuilder;
import fr.cnes.sirius.patrius.assembly.properties.AeroSphereProperty;
import fr.cnes.sirius.patrius.assembly.properties.MassProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeProperty;
import fr.cnes.sirius.patrius.assembly.properties.RadiativeSphereProperty;
import fr.cnes.sirius.patrius.math.parameter.Parameter;
import fr.cnes.sirius.patrius.utils.exception.PatriusException;

/**
 * Utility class to build Stela spacecrafts
 * 
 * @concurrency unconditionally thread-safe
 * @concurrency.comment access to the only static field is synchronized
 * 
 * @author Rami Houdroge
 * 
 * @version $Id$
 * 
 * @since 1.3
 */
public final class StelaSpacecraftFactory {

    /** Inner builder. */
    private static final AssemblyBuilder BUILDER = new AssemblyBuilder();

    /** Radiative area parameter name. */
    private static final String RADIATIVE_AREA = "RADIATIVE_AREA";

    /** Drag area parameter name. */
    private static final String DRAG_AREA = "DRAG_AREA";

    /** Nine. */
    private static final double NINE = 9.;
    
    /**
     * Private constructor
     */
    private StelaSpacecraftFactory() {
    }

    /**
     * <p>
     * Utility method to create a STELA Assembly, made of a sphere with both radiative and aerodynamic properties.
     * </p>
     * <p>
     * The reflection coefficient C<sub>r</sub> (parameter srpReflectionCoefficient) is related to the diffuse
     * reflection coefficient k<sub>d</sub> as per :
     * 
     * <pre>
     * k<sub>d</sub> = (C<sub>r</sub> - 1) * 9 / 4.
     * </pre>
     * 
     * @param mainPartName
     *        name of spacecraft
     * @param mass
     *        mass of spacecraft
     * @param dragArea
     *        drag area of spacecraft
     * @param dragCoefficient
     *        drag coefficient of spacecraft
     * @param srpArea
     *        radiative are of spacecraf
     * @param srpReflectionCoefficient
     *        reflection coefficient of spacecraft
     * @return the assembly
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public static Assembly
            createStelaCompatibleSpacecraft(final String mainPartName, final double mass,
                                            final double dragArea, final double dragCoefficient, final double srpArea,
                                            final double srpReflectionCoefficient) throws PatriusException {

        synchronized (BUILDER) {
            BUILDER.addMainPart(mainPartName);

            // mass
            BUILDER.addProperty(new MassProperty(mass), mainPartName);

            // SRP properties, see Radiative sphere property for coefficient formula
            BUILDER.addProperty(new RadiativeProperty(0, 0, (srpReflectionCoefficient - 1) * NINE / 4.), mainPartName);
            BUILDER.addProperty(new RadiativeSphereProperty(new Parameter(RADIATIVE_AREA, srpArea)), mainPartName);

            // Aero property
            BUILDER.addProperty(new AeroSphereProperty(new Parameter(DRAG_AREA, dragArea), dragCoefficient),
                mainPartName);

            return BUILDER.returnAssembly();
        }
    }

    /**
     * <p>
     * Utility method to create a STELA Assembly, made of a sphere with only radiative properties.
     * </p>
     * <p>
     * The reflection coefficient C<sub>r</sub> (parameter srpReflectionCoefficient) is related to the diffuse
     * reflection coefficient k<sub>d</sub> as per :
     * 
     * <pre>
     * k<sub>d</sub> = (C<sub>r</sub> - 1) * 9 / 4.
     * </pre>
     * 
     * @param mainPartName
     *        name of spacecraft
     * @param mass
     *        mass of spacecraft
     * @param srpArea
     *        radiative are of spacecraf
     * @param srpReflectionCoefficient
     *        reflection coefficient of spacecraft
     * @return the assembly
     * @throws PatriusException
     *         if the mass is negative (PatriusMessages.MASS_ARGUMENT_IS_NEGATIVE)
     */
    public static Assembly createStelaRadiativeSpacecraft(final String mainPartName, final double mass,
            final double srpArea, final double srpReflectionCoefficient) throws PatriusException {

        synchronized (BUILDER) {
            BUILDER.addMainPart(mainPartName);

            // mass
            BUILDER.addProperty(new MassProperty(mass), mainPartName);

            // SRP properties, see Radiative sphere property for coefficient formula
            BUILDER.addProperty(new RadiativeProperty(0, 0, (srpReflectionCoefficient - 1) * NINE / 4.), mainPartName);
            BUILDER.addProperty(new RadiativeSphereProperty(new Parameter(RADIATIVE_AREA, srpArea)), mainPartName);

            return BUILDER.returnAssembly();
        }
    }
}
