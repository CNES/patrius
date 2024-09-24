/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 *
 * Copyright 2011-2022 CNES
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
 * HISTORY
 * VERSION:4.10:DM:DM-3185:03/11/2022:[PATRIUS] Decoupage de Patrius en vue de la mise a disposition dans GitHub
 * VERSION:4.9:FA:FA-3128:10/05/2022:[PATRIUS] Historique des modifications et Copyrights 
 * VERSION:4.3:DM:DM-2097:15/05/2019: Mise en conformite du code avec le nouveau standard de codage DYNVOL
 * END-HISTORY
 */
package fr.cnes.sirius.patrius.math.exception.util;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class that contains the actual implementation of the functionality mandated
 * by the {@link ExceptionContext} interface.
 * All Commons Math exceptions delegate the interface's methods to this class.
 * 
 * @since 3.0
 * @version $Id: ExceptionContext.java 18108 2017-10-04 06:45:27Z bignon $
 */
public class ExceptionContext implements Serializable {
    /** Serializable UID. */
    private static final long serialVersionUID = -6024911025449780478L;
    /**
     * The throwable to which this context refers to.
     */
    private Throwable throwable;
    /**
     * Various informations that enrich the informative message.
     */
    private List<Localizable> msgPatterns;
    /**
     * Various informations that enrich the informative message.
     * The arguments will replace the corresponding place-holders in {@link #msgPatterns}.
     */
    private List<Object[]> msgArguments;
    /**
     * Arbitrary context information.
     */
    private Map<String, Object> context;

    /**
     * Simple constructor.
     * 
     * @param throwableIn
     *        the exception this context refers too
     */
    public ExceptionContext(final Throwable throwableIn) {
        this.throwable = throwableIn;
        this.msgPatterns = new ArrayList<>();
        this.msgArguments = new ArrayList<>();
        this.context = new HashMap<>();
    }

    /**
     * Get a reference to the exception to which the context relates.
     * 
     * @return a reference to the exception to which the context relates
     */
    public Throwable getThrowable() {
        return this.throwable;
    }

    /**
     * Adds a message.
     * 
     * @param pattern
     *        Message pattern.
     * @param arguments
     *        Values for replacing the placeholders in the message
     *        pattern.
     */
    public void addMessage(final Localizable pattern,
                           final Object... arguments) {
        this.msgPatterns.add(pattern);
        this.msgArguments.add(ArgUtils.flatten(arguments));
    }

    /**
     * Sets the context (key, value) pair.
     * Keys are assumed to be unique within an instance. If the same key is
     * assigned a new value, the previous one will be lost.
     * 
     * @param key
     *        Context key (not null).
     * @param value
     *        Context value.
     */
    public void setValue(final String key, final Object value) {
        this.context.put(key, value);
    }

    /**
     * Gets the value associated to the given context key.
     * 
     * @param key
     *        Context key.
     * @return the context value or {@code null} if the key does not exist.
     */
    public Object getValue(final String key) {
        return this.context.get(key);
    }

    /**
     * Gets all the keys stored in the exception
     * 
     * @return the set of keys.
     */
    public Set<String> getKeys() {
        return this.context.keySet();
    }

    /**
     * Gets the default message.
     * 
     * @return the message.
     */
    public String getMessage() {
        return this.getMessage(Locale.US);
    }

    /**
     * Gets the message in the default locale.
     * 
     * @return the localized message.
     */
    public String getLocalizedMessage() {
        return this.getMessage(Locale.getDefault());
    }

    /**
     * Gets the message in a specified locale.
     * 
     * @param locale
     *        Locale in which the message should be translated.
     * @return the localized message.
     */
    public String getMessage(final Locale locale) {
        return this.buildMessage(locale, ": ");
    }

    /**
     * Gets the message in a specified locale.
     * 
     * @param locale
     *        Locale in which the message should be translated.
     * @param separator
     *        Separator inserted between the message parts.
     * @return the localized message.
     */
    public String getMessage(final Locale locale,
                             final String separator) {
        return this.buildMessage(locale, separator);
    }

    /**
     * Builds a message string.
     * 
     * @param locale
     *        Locale in which the message should be translated.
     * @param separator
     *        Message separator.
     * @return a localized message string.
     */
    private String buildMessage(final Locale locale,
                                final String separator) {
        // new instance of stringBuilder
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        final int len = this.msgPatterns.size();
        // loop on the information messages
        for (int i = 0; i < len; i++) {
            final Localizable pat = this.msgPatterns.get(i);
            final Object[] args = this.msgArguments.get(i);
            // format the message with the locale
            final MessageFormat fmt = new MessageFormat(pat.getLocalizedString(locale),
                locale);
            sb.append(fmt.format(args));
            count++;
            if (count < len) {
                // Add a separator if there are other messages.
                sb.append(separator);
            }
        }

        return sb.toString();
    }

    /**
     * Serialize this object to the given stream.
     * 
     * @param ous
     *        Stream.
     * @throws IOException
     *         This should never happen.
     */
    private void writeObject(final ObjectOutputStream ous) throws IOException {
        ous.writeObject(this.throwable);
        this.serializeMessages(ous);
        this.serializeContext(ous);
    }

    /**
     * Deserialize this object from the given stream.
     * 
     * @param ois
     *        Stream.
     * @throws IOException
     *         This should never happen.
     * @throws ClassNotFoundException
     *         This should never happen.
     */
    private void readObject(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        this.throwable = (Throwable) ois.readObject();
        this.deSerializeMessages(ois);
        this.deSerializeContext(ois);
    }

    /**
     * Serialize {@link #msgPatterns} and {@link #msgArguments}.
     * 
     * @param oos
     *        Stream.
     * @throws IOException
     *         This should never happen.
     */
    private void serializeMessages(final ObjectOutputStream oos) throws IOException {
        // Step 1.
        final int len = this.msgPatterns.size();
        oos.writeInt(len);
        // Step 2.
        for (int i = 0; i < len; i++) {
            final Localizable pat = this.msgPatterns.get(i);
            // Step 3.
            oos.writeObject(pat);
            final Object[] args = this.msgArguments.get(i);
            final int aLen = args.length;
            // Step 4.
            oos.writeInt(aLen);
            for (int j = 0; j < aLen; j++) {
                if (args[j] instanceof Serializable) {
                    // Step 5a.
                    oos.writeObject(args[j]);
                } else {
                    // Step 5b.
                    oos.writeObject(this.nonSerializableReplacement(args[j]));
                }
            }
        }
    }

    /**
     * Deserialize {@link #msgPatterns} and {@link #msgArguments}.
     * 
     * @param ois
     *        Stream.
     * @throws IOException
     *         This should never happen.
     * @throws ClassNotFoundException
     *         This should never happen.
     */
    private void deSerializeMessages(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Step 1.
        final int len = ois.readInt();
        this.msgPatterns = new ArrayList<>(len);
        this.msgArguments = new ArrayList<>(len);
        // Step 2.
        for (int i = 0; i < len; i++) {
            // Step 3.
            final Localizable pat = (Localizable) ois.readObject();
            this.msgPatterns.add(pat);
            // Step 4.
            final int aLen = ois.readInt();
            final Object[] args = new Object[aLen];
            for (int j = 0; j < aLen; j++) {
                // Step 5.
                args[j] = ois.readObject();
            }
            this.msgArguments.add(args);
        }
    }

    /**
     * Serialize {@link #context}.
     * 
     * @param oos
     *        Stream.
     * @throws IOException
     *         This should never happen.
     */
    private void serializeContext(final ObjectOutputStream oos) throws IOException {
        // Step 1.
        final int len = this.context.keySet().size();
        oos.writeInt(len);
        for (final Entry<String, Object> entry : this.context.entrySet()) {
            final String key = entry.getKey();
            // Step 2.
            oos.writeObject(key);
            final Object value = this.context.get(key);
            if (value instanceof Serializable) {
                // Step 3a.
                oos.writeObject(value);
            } else {
                // Step 3b.
                oos.writeObject(this.nonSerializableReplacement(value));
            }
        }
    }

    /**
     * Deserialize {@link #context}.
     * 
     * @param ois
     *        Stream.
     * @throws IOException
     *         This should never happen.
     * @throws ClassNotFoundException
     *         This should never happen.
     */
    private void deSerializeContext(final ObjectInputStream ois) throws IOException, ClassNotFoundException {
        // Step 1.
        final int len = ois.readInt();
        this.context = new HashMap<>();
        for (int i = 0; i < len; i++) {
            // Step 2.
            final String key = (String) ois.readObject();
            // Step 3.
            final Object value = ois.readObject();
            this.context.put(key, value);
        }
    }

    /**
     * Replaces a non-serializable object with an error message string.
     * 
     * @param obj
     *        Object that does not implement the {@code Serializable} interface.
     * @return a string that mentions which class could not be serialized.
     */
    private String nonSerializableReplacement(final Object obj) {
        return "[Object could not be serialized: " + obj.getClass().getName() + "]";
    }
}
