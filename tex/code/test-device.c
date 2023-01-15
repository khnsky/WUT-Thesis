#include "qemu/osdep.h"

#include "hw/qdev-core.h"
#include "qom/object.h"

#include <stdio.h>

#define TYPE_TEST "test-device"

struct TestState {
    DeviceState parent;
};

struct TestClass {
    DeviceClass parent;
};

OBJECT_DECLARE_TYPE(TestState, TestClass, TEST)

static void test_class_init(ObjectClass* objectClass, void* data) {
    TestClass* testClass = TEST_CLASS(objectClass);

    printf("test_class_init: testClass = %p\n", testClass);
}

static void test_instance_init(Object* object) {
    TestState* testState = TEST(object);

    printf("test_instance_init: testState = %p\n", testState);
}

static void test_instance_finalize(Object* object) {
    TestState* testState = TEST(object);

    printf("test_instance_finalize: testState = %p\n", testState);
}

static const TypeInfo test_info = {
    .name               = TYPE_TEST,
    .parent             = TYPE_DEVICE,
    .instance_size      = sizeof(TestState),
    .instance_init      = test_instance_init,
    .instance_finalize  = test_instance_finalize,
    .class_size         = sizeof(TestClass),
    .class_init         = test_class_init,
};


static void test_register_types(void) {
    type_register_static(&test_info);
}

type_init(test_register_types)
