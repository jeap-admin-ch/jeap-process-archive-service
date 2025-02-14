package ch.admin.bit.jeap.processarchive.configuration.json.deserializer;

import lombok.experimental.UtilityClass;

@UtilityClass
class Instances {

    <T> T newInstance(String className) {
        if (className == null) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Class<T> conditionClass = (Class<T>) Class.forName(className);
            return conditionClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw DomainEventArchiveConfigurationException.errorWhileCreatingInstance(className, e);
        }
    }

}
