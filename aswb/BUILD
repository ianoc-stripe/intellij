#
# Description: Builds ASwB for blaze and bazel
#

load(
    "//build_defs:build_defs.bzl",
    "intellij_plugin",
    "intellij_plugin_library",
    "optional_plugin_xml",
    "plugin_deploy_zip",
    "repackaged_files",
    "stamped_plugin_xml",
)
load(
    "//build_defs:intellij_plugin_debug_target.bzl",
    "intellij_plugin_debug_target",
)
load("//:version.bzl", "VERSION")
load("//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")
load(
    "//testing:test_defs.bzl",
    "intellij_integration_test_suite",
    "intellij_unit_test_suite",
)

licenses(["notice"])  # Apache 2.0

optional_plugin_xml(
    name = "optional_ndk_xml",
    module = "com.android.tools.ndk",
    plugin_xml = "src/META-INF/ndk_contents.xml",
)

intellij_plugin_library(
    name = "plugin_library",
    optional_plugin_xmls = [":optional_ndk_xml"],
    plugin_xmls = ["src/META-INF/aswb.xml"],
    deps = [":aswb_lib"],
)

stamped_plugin_xml(
    name = "stamped_plugin_xml",
    changelog_file = "//:changelog",
    description_file = "src/META-INF/description.html",
    plugin_id = "com.google.idea.bazel.aswb",
    plugin_name = "Bazel",
    stamp_since_build = True,
    stamp_until_build = True,
    version = VERSION,
)

java_library(
    name = "unit_test_utils",
    testonly = 1,
    srcs = glob(["tests/utils/unit/**/*.java"]) + select_for_plugin_api({
        "android-studio-3.6": glob(["testcompat/as36/utils/unit/**/*.java"]),
        "android-studio-4.0": glob(["testcompat/as40/utils/unit/**/*.java"]),
        "android-studio-4.1": glob(["testcompat/as41/utils/unit/**/*.java"]),
    }),
    deps = [
        ":aswb_lib",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
    ],
)

java_library(
    name = "integration_test_utils",
    testonly = 1,
    srcs = glob(["tests/utils/integration/**/*.java"]) + select_for_plugin_api({
        "android-studio-3.6": glob(["testcompat/as36/utils/integration/**/*.java"]),
        "android-studio-4.0": glob(["testcompat/as40/utils/integration/**/*.java"]),
        "android-studio-4.1": glob(["testcompat/as41/utils/integration/**/*.java"]),
    }),
    deps = [
        "//base",
        "//base:integration_test_utils",
        "//base:unit_test_utils",
        "//cpp",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
        "//java",
        "//testing:lib",
        "@junit//jar",
    ],
)

java_library(
    name = "aswb_lib",
    srcs = glob(["src/**/*.java"]) + select_for_plugin_api({
        "android-studio-3.6": glob(["sdkcompat/as36/**/*.java"]),
        "android-studio-4.0": glob(["sdkcompat/as40/**/*.java"]),
        "android-studio-4.1": glob(["sdkcompat/as41/**/*.java"]),
    }),
    resources = glob(["resources/**/*"]),
    deps = [
        "//base",
        "//common/experiments",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api",
        "//java",
        "//proto:proto_deps",
    ],
)

intellij_unit_test_suite(
    name = "unit_tests",
    srcs = glob(["tests/unittests/**/*.java"]) + select_for_plugin_api({
        "android-studio-3.6": glob(["testcompat/as36/unittests/**/*.java"]),
        "android-studio-4.0": glob(["testcompat/as40/unittests/**/*.java"]),
        "android-studio-4.1": glob(["testcompat/as41/unittests/**/*.java"]),
    }),
    test_package_root = "com.google.idea.blaze.android",
    deps = [
        ":aswb_lib",
        ":unit_test_utils",
        "//base",
        "//base:unit_test_utils",
        "//common/experiments",
        "//common/experiments:unit_test_utils",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
        "//java",
        "//proto:proto_deps",
        "@junit//jar",
    ],
)

test_suite(
    name = "integration_tests",
    tests = [
        #b/139825934 ":NdkDependenciesTest",
        ":normal_integration_tests",
    ],
)

intellij_integration_test_suite(
    name = "normal_integration_tests",
    srcs = glob(
        ["tests/integrationtests/**/*.java"],
        exclude = [
            "tests/integrationtests/com/google/idea/blaze/android/plugin/NdkDependenciesTest.java",
            "tests/integrationtests/com/google/idea/blaze/android/functional/AswbMergedManifestTest.java",
            "tests/integrationtests/com/google/idea/blaze/android/AswbGotoDeclarationTest.java",
        ],
    ) + select_for_plugin_api({
        "android-studio-3.6": glob(["testcompat/as36/integration/**/*.java"]),
        "android-studio-4.0": glob(["testcompat/as40/integration/**/*.java"]),
        "android-studio-4.1": glob(["testcompat/as41/integration/**/*.java"]),
    }),
    required_plugins = "com.google.idea.bazel.aswb",
    test_package_root = "com.google.idea.blaze.android",
    runtime_deps = [
        ":aswb_bazel",
        # Needed to work around one-version issue
        "//intellij_platform_sdk:test_libs",
    ],
    deps = [
        ":aswb_lib",
        ":integration_test_utils",
        "//base",
        "//base:integration_test_utils",
        "//base:unit_test_utils",
        "//common/experiments",
        "//common/experiments:unit_test_utils",
        "//cpp",
        "//intellij_platform_sdk:jsr305",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
        "//java",
        "//proto:proto_deps",
        "@junit//jar",
    ],
)

intellij_integration_test_suite(
    name = "NdkDependenciesTest",
    srcs = ["tests/integrationtests/com/google/idea/blaze/android/plugin/NdkDependenciesTest.java"],
    required_plugins = "com.google.idea.bazel.aswb",
    test_package_root = "com.google.idea.blaze.android",
    runtime_deps = [
        ":aswb_bazel",
        "//cpp",
        "//java",
    ],
    deps = [
        ":aswb_lib",
        "//base",
        "//base:integration_test_utils",
        "//base:unit_test_utils",
        "//intellij_platform_sdk:plugin_api_for_tests",
        "//intellij_platform_sdk:test_libs",
        "//proto:proto_deps",
        "@junit//jar",
    ],
)

intellij_plugin(
    name = "aswb_bazel",
    plugin_xml = ":stamped_plugin_xml",
    deps = [
        ":plugin_library",
        "//base:plugin_library",
        "//cpp:plugin_library",
        "//dart:plugin_library",
        "//java:plugin_library",
        "//kotlin:plugin_library",
        "//python:plugin_library",
        "//skylark:plugin_library",
        "//terminal:plugin_library",
    ],
)

repackaged_files(
    name = "plugin_jar",
    srcs = [":aswb_bazel"],
    prefix = "aswb/lib",
)

repackaged_files(
    name = "aspect_directory",
    srcs = ["//aspect:aspect_files"],
    prefix = "aswb/aspect",
    strip_prefix = "/aspect",
)

repackaged_files(
    name = "fast_build_javac",
    srcs = ["//java:fast_build_javac"],
    prefix = "aswb/lib",
)

intellij_plugin_debug_target(
    name = "aswb_bazel_dev",
    deps = [
        ":aspect_directory",
        ":fast_build_javac",
        ":plugin_jar",
    ],
)

plugin_deploy_zip(
    name = "aswb_bazel_zip",
    srcs = [
        ":aspect_directory",
        ":fast_build_javac",
        ":plugin_jar",
    ],
    zip_filename = "aswb_bazel.zip",
)
