// Copyright 2015 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.analysis.config;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.devtools.build.lib.cmdline.Label;
import com.google.devtools.common.options.Converter;
import com.google.devtools.common.options.Option;
import com.google.devtools.common.options.OptionDocumentationCategory;
import com.google.devtools.common.options.OptionEffectTag;
import com.google.devtools.common.options.OptionMetadataTag;
import com.google.devtools.common.options.OptionsParser;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link BuildOptionDetails}. */
@RunWith(JUnit4.class)
public class BuildOptionDetailsTest {
  /** Instantiates the given options classes, parsing the given options as well. */
  public Iterable<FragmentOptions> parseOptions(
      Iterable<? extends Class<? extends FragmentOptions>> optionsClasses, String... options)
      throws Exception {
    OptionsParser optionsParser =
        OptionsParser.builder().optionsClasses(optionsClasses).allowResidue(false).build();
    optionsParser.parse(options);
    ImmutableList.Builder<FragmentOptions> output = new ImmutableList.Builder<>();
    for (Class<? extends FragmentOptions> optionsClass : optionsClasses) {
      output.add(optionsParser.getOptions(optionsClass));
    }
    return output.build();
  }

  /** Example converter for working with options with converters. */
  public static final class Optionalizer extends Converter.Contextless<Optional<String>> {
    @Override
    public Optional<String> convert(String input) {
      if ("".equals(input)) {
        return Optional.<String>absent();
      }
      return Optional.of(input);
    }

    @Override
    public String getTypeDescription() {
      return "a string";
    }
  }

  /** Example options class for testing options lookup. */
  public static final class Options extends FragmentOptions {
    @Option(
        name = "boolean_option",
        documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
        effectTags = {OptionEffectTag.NO_OP},
        defaultValue = "true")
    public boolean booleanOption;

    @Option(
        name = "convertible_option",
        converter = Optionalizer.class,
        documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
        effectTags = {OptionEffectTag.NO_OP},
        defaultValue = "")
    public Optional<String> convertibleOption;

    @Option(
        name = "null_default",
        documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
        effectTags = {OptionEffectTag.NO_OP},
        defaultValue = "null")
    public String nullDefault;

    @Option(
        name = "late_bound_default",
        documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
        effectTags = {OptionEffectTag.NO_OP},
        defaultValue = "null")
    public String lateBoundDefault;

    @Option(
        name = "multi_option",
        documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
        effectTags = {OptionEffectTag.NO_OP},
        defaultValue = "null",
        allowMultiple = true)
    public List<String> multiOption;

    @Option(
        name = "internal option",
        defaultValue = "secret",
        documentationCategory = OptionDocumentationCategory.UNDOCUMENTED,
        effectTags = {OptionEffectTag.NO_OP},
        metadataTags = {OptionMetadataTag.INTERNAL})
    public String internalOption;

    @Option(
        name = "internal multi option",
        documentationCategory = OptionDocumentationCategory.UNDOCUMENTED,
        effectTags = {OptionEffectTag.NO_OP},
        defaultValue = "null",
        metadataTags = {OptionMetadataTag.INTERNAL},
        allowMultiple = true)
    public List<String> internalMultiOption;
  }

  /** Additional options class for testing options lookup. */
  public static final class MoreOptions extends FragmentOptions {
    @Option(
        name = "other_option",
        documentationCategory = OptionDocumentationCategory.UNCATEGORIZED,
        effectTags = {OptionEffectTag.NO_OP},
        defaultValue = "")
    public String otherOption;
  }

  @Test
  public void getOptionClass_ReturnsClassOfPresentOptions() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionClass("boolean_option")).isEqualTo(Options.class);
  }

  @Test
  public void getOptionClass_SelectsCorrectClassWhenMultipleArePresent() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(
            parseOptions(ImmutableList.of(Options.class, MoreOptions.class)));
    assertThat(details.getOptionClass("boolean_option")).isEqualTo(Options.class);
    assertThat(details.getOptionClass("other_option")).isEqualTo(MoreOptions.class);
  }

  @Test
  public void getOptionClass_ReturnsNullIfOptionsClassIsNotPartOfOptionDetails() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionClass("other_option")).isNull();
  }

  @Test
  public void getOptionClass_SelectsCorrectClassEvenWhenValueIsNull() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionClass("null_default")).isEqualTo(Options.class);
  }

  @Test
  public void getOptionClass_ReturnsNullWhenOptionIsUndefined() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionClass("undefined_option")).isNull();
  }

  @Test
  public void getOptionClass_ReturnsNullIfOptionIsInternal() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionClass("internal option")).isNull();
  }

  @Test
  public void getOptionValue_ReturnsDefaultValueIfNotSet() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionValue("boolean_option")).isEqualTo(true);
  }

  @Test
  public void getOptionValue_ReturnsCommandLineValueIfSet() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(
            parseOptions(ImmutableList.of(Options.class), "--noboolean_option"));
    assertThat(details.getOptionValue("boolean_option")).isEqualTo(false);
  }

  @Test
  public void getOptionValue_ReturnsEmptyListForUnspecifiedMultiOptions() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(
            parseOptions(ImmutableList.of(Options.class), "--noboolean_option"));
    assertThat(details.getOptionValue("multi_option")).isEqualTo(ImmutableList.<String>of());
  }

  @Test
  public void getOptionValue_ReturnsListOfValuesForSpecifiedMultiOptions() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(
            parseOptions(
                ImmutableList.of(Options.class),
                "--multi_option=one",
                "--multi_option=2",
                "--multi_option=iii"));
    assertThat(details.getOptionValue("multi_option"))
        .isEqualTo(ImmutableList.of("one", "2", "iii"));
  }

  @Test
  public void getOptionValue_DrawsValuesFromAllOptionsClasses() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(
            parseOptions(ImmutableList.of(Options.class, MoreOptions.class), "--other_option=set"));
    assertThat(details.getOptionValue("other_option")).isEqualTo("set");
  }

  @Test
  public void getOptionValue_UsesConvertersIfSpecified() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(
            parseOptions(ImmutableList.of(Options.class), "--convertible_option=Set"));
    assertThat(details.getOptionValue("convertible_option")).isEqualTo(Optional.of("Set"));
  }

  @Test
  public void getOptionValue_UsesConvertersForDefaultsIfSpecified() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionValue("convertible_option")).isEqualTo(Optional.<String>absent());
  }

  @Test
  public void getOptionValue_ReturnsNullIfOptionIsNotDefined() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionValue("undefined_option")).isNull();
  }

  @Test
  public void getOptionValue_ReturnsNullIfOptionIsInternal() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionValue("internal option")).isNull();
  }

  @Test
  public void getOptionValue_ReturnsNullIfOptionIsDefinedInNonIncludedOptionsClass()
      throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionValue("other_option")).isNull();
  }

  @Test
  public void getOptionValue_ReturnsNullIfOptionDefaultValueIsNull() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.getOptionValue("null_option")).isNull();
  }

  @Test
  public void allowsMultipleValues_ReturnsFalseForUndefinedOption() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.allowsMultipleValues("undefined_option")).isFalse();
  }

  @Test
  public void allowsMultipleValues_ReturnsFalseForNonMultiOption() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.allowsMultipleValues("boolean_option")).isFalse();
  }

  @Test
  public void allowsMultipleValues_ReturnsFalseForInternalNonMultiOption() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.allowsMultipleValues("internal option")).isFalse();
  }

  @Test
  public void allowsMultipleValues_ReturnsFalseForInternalMultiOption() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.allowsMultipleValues("internal multi option")).isFalse();
  }

  @Test
  public void allowsMultipleValues_ReturnsTrueForMultiOption() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptionsForTesting(parseOptions(ImmutableList.of(Options.class)));
    assertThat(details.allowsMultipleValues("multi_option")).isTrue();
  }

  @Test
  public void starlarkOptions() throws Exception {
    BuildOptionDetails details =
        BuildOptionDetails.forOptions(
            parseOptions(ImmutableList.of(Options.class)),
            ImmutableMap.of(Label.parseCanonicalUnchecked("//test:setting"), "value"));
    assertThat(details.getOptionValue(Label.parseCanonicalUnchecked("//test:setting")))
        .isEqualTo("value");
  }
}
