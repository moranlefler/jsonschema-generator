/*
 * Copyright 2019 VicTools.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.victools.jsonschema.generator;

import com.fasterxml.classmate.ResolvedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Generic collection of reflection based analysis for populating a JSON Schema from a certain kind of member.
 *
 * @param <M> type of the (resolved) member to analyse
 */
public class SchemaGeneratorConfigPart<M extends MemberScope<?, ?>> extends SchemaGeneratorTypeConfigPart<M> {

    private final List<InstanceAttributeOverride<M>> instanceAttributeOverrides = new ArrayList<>();

    /*
     * Customising options for properties in a schema with "type": object;
     * either skipping them completely, marking them as required or also allowing for "type": "null".
     */
    private final List<Predicate<M>> ignoreChecks = new ArrayList<>();
    private final List<Predicate<M>> requiredChecks = new ArrayList<>();
    private final List<ConfigFunction<M, Boolean>> nullableChecks = new ArrayList<>();

    /*
     * Customising options for the names of properties in a schema with "type": "object".
     */
    private final List<ConfigFunction<M, ResolvedType>> targetTypeOverrideResolvers = new ArrayList<>();
    private final List<ConfigFunction<M, String>> propertyNameOverrideResolvers = new ArrayList<>();

    /**
     * Setter for override of attributes on a given JSON Schema node in the respective member.
     *
     * @param override override of a given JSON Schema node's instance attributes
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withInstanceAttributeOverride(InstanceAttributeOverride<M> override) {
        this.instanceAttributeOverrides.add(override);
        return this;
    }

    /**
     * Getter for the applicable instance attribute overrides.
     *
     * @return overrides of a given JSON Schema node's instance attributes
     */
    public List<InstanceAttributeOverride<M>> getInstanceAttributeOverrides() {
        return Collections.unmodifiableList(this.instanceAttributeOverrides);
    }

    /**
     * Setter for ignore check.
     *
     * @param check how to determine whether a given reference should be ignored
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withIgnoreCheck(Predicate<M> check) {
        this.ignoreChecks.add(check);
        return this;
    }

    /**
     * Determine whether a given member should be included – ignoring member if any inclusion check returns false.
     *
     * @param member member to check
     * @return whether the member should be ignored (defaults to false)
     */
    public boolean shouldIgnore(M member) {
        return this.ignoreChecks.stream().anyMatch(check -> check.test(member));
    }

    /**
     * Setter for required check.
     *
     * @param check how to determine whether a given reference should have required value
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withRequiredCheck(Predicate<M> check) {
        this.requiredChecks.add(check);
        return this;
    }

    /**
     * Determine whether a given member should be indicated as being required in its declaring type.
     *
     * @param member member to check
     * @return whether the member is required (defaults to false)
     */
    public boolean isRequired(M member) {
        return this.requiredChecks.stream().anyMatch(check -> check.test(member));
    }

    /**
     * Setter for nullable check.
     *
     * @param check how to determine whether a given reference should be nullable
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withNullableCheck(ConfigFunction<M, Boolean> check) {
        this.nullableChecks.add(check);
        return this;
    }

    /**
     * Determine whether a given member is nullable.
     *
     * @param member member to check
     * @return whether the member is nullable (may be null if not specified)
     */
    public Boolean isNullable(M member) {
        Set<Boolean> result = this.nullableChecks.stream()
                .map(check -> check.apply(member))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return result.isEmpty() ? null : result.stream().anyMatch(value -> value);
    }

    /**
     * Setter for target type resolver, expecting the respective member and the default type as inputs.
     * <br>
     * For generally replacing one type with one or multiple of its subtypes, you may want to consider adding a {@link SubtypeResolver} via
     * {@link SchemaGeneratorConfigBuilder#forTypesInGeneral() forTypesInGeneral()} instead.
     *
     * @param resolver how to determine the alternative target type
     * @return this config part (for chaining)
     * @see SchemaGeneratorGeneralConfigPart#withSubtypeResolver(SubtypeResolver)
     */
    public SchemaGeneratorConfigPart<M> withTargetTypeOverrideResolver(ConfigFunction<M, ResolvedType> resolver) {
        this.targetTypeOverrideResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the alternative target type from a given member.
     *
     * @param member member to determine the target type override for
     * @return target type of member (may be null)
     * @see MemberScope#getDeclaredType()
     * @see MemberScope#getOverriddenType()
     */
    public ResolvedType resolveTargetTypeOverride(M member) {
        return getFirstDefinedValue(this.targetTypeOverrideResolvers, member);
    }

    /**
     * Setter for property name resolver, expecting the respective member and the default name as inputs.
     *
     * @param resolver how to determine the alternative name in a parent JSON Schema's "properties"
     * @return this config part (for chaining)
     */
    public SchemaGeneratorConfigPart<M> withPropertyNameOverrideResolver(ConfigFunction<M, String> resolver) {
        this.propertyNameOverrideResolvers.add(resolver);
        return this;
    }

    /**
     * Determine the alternative name in a parent JSON Schema's "properties" from a given member.
     *
     * @param member member to determine the property name for
     * @return name in a parent JSON Schema's "properties" (may be null, thereby falling back on the default value)
     */
    public String resolvePropertyNameOverride(M member) {
        return getFirstDefinedValue(this.propertyNameOverrideResolvers, member);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withTitleResolver(ConfigFunction<M, String> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withTitleResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withDescriptionResolver(ConfigFunction<M, String> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withDescriptionResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withDefaultResolver(ConfigFunction<M, Object> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withDefaultResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withEnumResolver(ConfigFunction<M, Collection<?>> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withEnumResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withAdditionalPropertiesResolver(ConfigFunction<M, Type> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withAdditionalPropertiesResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withPatternPropertiesResolver(ConfigFunction<M, Map<String, Type>> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withPatternPropertiesResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withStringMinLengthResolver(ConfigFunction<M, Integer> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withStringMinLengthResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withStringMaxLengthResolver(ConfigFunction<M, Integer> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withStringMaxLengthResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withStringFormatResolver(ConfigFunction<M, String> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withStringFormatResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withStringPatternResolver(ConfigFunction<M, String> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withStringPatternResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withNumberInclusiveMinimumResolver(ConfigFunction<M, BigDecimal> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withNumberInclusiveMinimumResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withNumberExclusiveMinimumResolver(ConfigFunction<M, BigDecimal> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withNumberExclusiveMinimumResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withNumberInclusiveMaximumResolver(ConfigFunction<M, BigDecimal> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withNumberInclusiveMaximumResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withNumberExclusiveMaximumResolver(ConfigFunction<M, BigDecimal> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withNumberExclusiveMaximumResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withNumberMultipleOfResolver(ConfigFunction<M, BigDecimal> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withNumberMultipleOfResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withArrayMinItemsResolver(ConfigFunction<M, Integer> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withArrayMinItemsResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withArrayMaxItemsResolver(ConfigFunction<M, Integer> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withArrayMaxItemsResolver(resolver);
    }

    @Override
    public SchemaGeneratorConfigPart<M> withArrayUniqueItemsResolver(ConfigFunction<M, Boolean> resolver) {
        return (SchemaGeneratorConfigPart<M>) super.withArrayUniqueItemsResolver(resolver);
    }
}
