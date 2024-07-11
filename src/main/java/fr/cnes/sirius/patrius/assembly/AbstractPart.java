/**
 * 
 * Copyright 2011-2022 CNES
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
 * HISTORY
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.4:DM:DM-2148:04/10/2019:[PATRIUS] Creations de parties mobiles dans un Assembly
 * VERSION:4.4:DM:DM-2097:04/10/2019:[PATRIUS] Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.assembly;

import java.util.LinkedHashMap;
import java.util.Map;

import fr.cnes.sirius.patrius.utils.exception.PatriusException;
import fr.cnes.sirius.patrius.utils.exception.PatriusMessages;

/**
 * Abstract part: class gathering all common methods of assmelby parts.
 * 
 * @author Emmanuel Bignon
 * 
 * @since 4.4
 */
public abstract class AbstractPart implements IPart {

    /** Serial UID. */
    private static final long serialVersionUID = 3963926051171285088L;

    /** Part name. */
    private final String name;

    /** Parent part. */
    private final IPart parentPart;

    /** List of properties. */
    private final Map<PropertyType, IPartProperty> properties;

    /** Part level in the assembly's tree. */
    private final int partLevel;

    /**
     * Constructor.
     * @param name the name of the part
     * @param parentPart the parent part
     */
    public AbstractPart(final String name, final IPart parentPart) {
        this.name = name;
        this.parentPart = parentPart;
        this.properties = new LinkedHashMap<PropertyType, IPartProperty>();
        if (parentPart == null) {
            this.partLevel = 0;
        } else {
            this.partLevel = this.parentPart.getPartLevel() + 1;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean hasProperty(final PropertyType propertyType) {
        return properties.containsKey(propertyType);
    }

    /** {@inheritDoc} */
    @Override
    public final void addProperty(final IPartProperty property) {
        if (!properties.containsKey(property.getType())) {
            properties.put(property.getType(), property);
        } else {
            // if a property of this type already exists, it can't be added
            throw PatriusException.createIllegalArgumentException(PatriusMessages.PDB_PROPERTY_ALREADY_EXIST);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final IPartProperty getProperty(final PropertyType propertyType) {
        if (properties.containsKey(propertyType)) {
            return properties.get(propertyType);
        } else {
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final String getName() {
        return name;
    }

    /** {@inheritDoc} */
    @Override
    public final IPart getParent() {
        return parentPart;
    }

    /** {@inheritDoc} */
    @Override
    public final int getPartLevel() {
        return partLevel;
    }
}
