/*
 * Minecraft Forge, Patchwork Project
 * Copyright (c) 2016-2019, 2019
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fml.common;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.StringJoiner;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import cpw.mods.modlauncher.api.INameMappingService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import com.patchworkmc.impl.fml.PatchworkMappingResolver;

/**
 * Some reflection helper code.
 * This may not work properly in Java 9 with its new, more restrictive, reflection management.
 * As such, if issues are encountered, please report them and we can see what we can do to expand
 * the compatibility.
 *
 * <p>In other cases, accesssor mixins may be used.</p>
 *
 * <p>All field and method names should be passed in as intermediary names, and this will automatically resolve if Yarn mappings are detected.</p>
 *
 */
public class ObfuscationReflectionHelper {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final Marker REFLECTION = MarkerManager.getMarker("REFLECTION");

	/**
	 * Remaps a class name using {@link PatchworkMappingResolver#remapName(INameMappingService.Domain, Class, String)}.
	 * Throws an exception for non-class domains.
	 *
	 * @param domain The {@link INameMappingService.Domain} to use to remap the name.
	 * @param name   The name to try and remap.
	 * @return The remapped name, or the original name if it couldn't be remapped.
	 * @throws UnsupportedOperationException if the {@code domain} is not {@link INameMappingService.Domain#CLASS}
	 * @deprecated Patchwork: use {@link PatchworkMappingResolver} instead
	 */
	@Deprecated
	@Nonnull
	public static String remapName(INameMappingService.Domain domain, String name) {
		if (domain == INameMappingService.Domain.CLASS) {
			return PatchworkMappingResolver.remapName(domain, null, name);
		} else {
			// This would need a special remapping system, if someone uses it then it can be implemented
			throw new UnsupportedOperationException("FIXME: Unable to lookup member of type " + domain.name() + " with name " + name + ".");
		}
	}

	/**
	 * Gets the value a field with the specified index in the given class.
	 * Note: For performance, use {@link #findField(Class, int)} if you are getting the value more than once.
	 *
	 * <p>Throws an exception if the field is not found or the value of the field cannot be gotten.</p>
	 *
	 * @param classToAccess The class to find the field on.
	 * @param instance      The instance of the {@code classToAccess}.
	 * @param fieldIndex    The index of the field in the {@code classToAccess}.
	 * @param <T>           The type of the value.
	 * @param <E>           The type of the {@code classToAccess}.
	 * @return The value of the field with the specified index in the {@code classToAccess}.
	 * @throws UnableToAccessFieldException If there was a problem getting the field or the value.
	 * @deprecated Use {@link #getPrivateValue(Class, Object, String)} because field indices change a lot more often than field names do.
	 * Forge: TODO Remove in 1.15
	 */
	@Deprecated
	@Nullable
	public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, int fieldIndex) {
		try {
			//noinspection unchecked
			return (T) findField(classToAccess, fieldIndex).get(instance);
		} catch (Exception e) {
			LOGGER.error(REFLECTION, "There was a problem getting field index {} from {}", fieldIndex, classToAccess.getName(), e);
			throw new UnableToAccessFieldException(e);
		}
	}

	/**
	 * Gets the value a field with the specified name in the given class.
	 * Note: For performance, use {@link #findField(Class, String)} if you are getting the value more than once.
	 *
	 * <p>Throws an exception if the field is not found or the value of the field cannot be gotten.</p>
	 *
	 * @param classToAccess The class to find the field on.
	 * @param instance      The instance of the {@code classToAccess}.
	 * @param fieldName     The intermediary (unmapped) name of the field to find (e.g. "field_5821").
	 * @param <T>           The type of the value.
	 * @param <E>           The type of the {@code classToAccess}.
	 * @return The value of the field with the specified name in the {@code classToAccess}.
	 * @throws UnableToAccessFieldException If there was a problem getting the field.
	 * @throws UnableToAccessFieldException If there was a problem getting the value.
	 */
	@Nullable
	public static <T, E> T getPrivateValue(Class<? super E> classToAccess, E instance, String fieldName) {
		try {
			//noinspection unchecked
			return (T) findField(classToAccess, fieldName).get(instance);
		} catch (UnableToFindFieldException e) {
			LOGGER.error(REFLECTION, "Unable to locate field {} ({}) on type {}", fieldName,
					PatchworkMappingResolver.remapName(INameMappingService.Domain.FIELD, classToAccess, fieldName), classToAccess.getName(), e);
			throw e;
		} catch (IllegalAccessException e) {
			LOGGER.error(REFLECTION, "Unable to access field {} ({}) on type {}", fieldName,
					PatchworkMappingResolver.remapName(INameMappingService.Domain.FIELD, classToAccess, fieldName), classToAccess.getName(), e);
			throw new UnableToAccessFieldException(e);
		}
	}

	/**
	 * Sets the value a field with the specified index in the given class.
	 * Note: For performance, use {@link #findField(Class, int)} if you are setting the value more than once.
	 *
	 * <p>Throws an exception if the field is not found or the value of the field cannot be set.</p>
	 *
	 * @param classToAccess The class to find the field on.
	 * @param instance      The instance of the {@code classToAccess}.
	 * @param value         The new value for the field
	 * @param fieldIndex    The index of the field in the {@code classToAccess}.
	 * @param <T>           The type of the value.
	 * @param <E>           The type of the {@code classToAccess}.
	 * @throws UnableToAccessFieldException If there was a problem setting the value of the field.
	 * @deprecated Use {@link #setPrivateValue(Class, Object, Object, String)} because field indices change a lot more often than field names do.
	 * Forge: TODO Remove in 1.15
	 */
	@Deprecated
	public static <T, E> void setPrivateValue(@Nonnull final Class<? super T> classToAccess, @Nonnull final T instance, @Nullable final E value, int fieldIndex) {
		try {
			findField(classToAccess, fieldIndex).set(instance, value);
		} catch (IllegalAccessException e) {
			LOGGER.error("There was a problem setting field index {} on type {}", fieldIndex, classToAccess.getName(), e);
			throw new UnableToAccessFieldException(e);
		}
	}

	/**
	 * Sets the value a field with the specified name in the given class.
	 * Note: For performance, use {@link #findField(Class, String)} if you are setting the value more than once.
	 *
	 * <p>Throws an exception if the field is not found or the value of the field cannot be set.</p>
	 *
	 * @param classToAccess The class to find the field on.
	 * @param instance      The instance of the {@code classToAccess}.
	 * @param value         The new value for the field
	 * @param fieldName     The name of the field in the {@code classToAccess}.
	 * @param <T>           The type of the value.
	 * @param <E>           The type of the {@code classToAccess}.
	 * @throws UnableToFindFieldException   If there was a problem getting the field.
	 * @throws UnableToAccessFieldException If there was a problem setting the value of the field.
	 */
	public static <T, E> void setPrivateValue(@Nonnull final Class<? super T> classToAccess, @Nonnull final T instance,
				@Nullable final E value, @Nonnull final String fieldName) {
		try {
			findField(classToAccess, fieldName).set(instance, value);
		} catch (UnableToFindFieldException e) {
			LOGGER.error("Unable to locate any field {} on type {}", fieldName, classToAccess.getName(), e);
			throw e;
		} catch (IllegalAccessException e) {
			LOGGER.error("Unable to set any field {} on type {}", fieldName, classToAccess.getName(), e);
			throw new UnableToAccessFieldException(e);
		}
	}

	/**
	 * Finds a method with the specified name and parameters in the given class and makes it accessible.
	 * Note: For performance, store the returned value and avoid calling this repeatedly.
	 *
	 * <p>Throws an exception if the method is not found.</p>
	 *
	 * @param clazz          The class to find the method on.
	 * @param methodName     The intermediary (unmapped) name of the method to find (e.g. "method_342").
	 * @param parameterTypes The parameter types of the method to find.
	 * @return The method with the specified name and parameters in the given class.
	 * @throws NullPointerException        If {@code clazz} is null.
	 * @throws NullPointerException        If {@code methodName} is null.
	 * @throws IllegalArgumentException    If {@code methodName} is empty.
	 * @throws NullPointerException        If {@code parameterTypes} is null.
	 * @throws UnableToFindMethodException If the method could not be found.
	 */
	@Nonnull
	public static Method findMethod(@Nonnull final Class<?> clazz, @Nonnull final String methodName, @Nonnull final Class<?>... parameterTypes) {
		Preconditions.checkNotNull(clazz, "Class to find method on cannot be null.");
		Preconditions.checkNotNull(methodName, "Name of method to find cannot be null.");
		Preconditions.checkArgument(!methodName.isEmpty(), "Name of method to find cannot be empty.");
		Preconditions.checkNotNull(parameterTypes, "Parameter types of method to find cannot be null.");

		try {
			String name = PatchworkMappingResolver.remapName(INameMappingService.Domain.METHOD, clazz, methodName);
			Method method = clazz.getDeclaredMethod(name, parameterTypes);
			method.setAccessible(true);
			return method;
		} catch (Exception e) {
			throw new UnableToFindMethodException(e);
		}
	}

	/**
	 * Finds a constructor with the specified parameter types in the given class and makes it accessible.
	 * Note: For performance, store the returned value and avoid calling this repeatedly.
	 *
	 * <p>Throws an exception if the constructor is not found.</p>
	 *
	 * @param clazz          The class to find the constructor in.
	 * @param parameterTypes The parameter types of the constructor.
	 * @param <T>            The type.
	 * @return The constructor with the specified parameters in the given class.
	 * @throws NullPointerException        If {@code clazz} is null.
	 * @throws NullPointerException        If {@code parameterTypes} is null.
	 * @throws UnknownConstructorException If the constructor could not be found.
	 */
	@Nonnull
	public static <T> Constructor<T> findConstructor(@Nonnull final Class<T> clazz, @Nonnull final Class<?>... parameterTypes) {
		Preconditions.checkNotNull(clazz, "Class to find constructor on cannot be null.");
		Preconditions.checkNotNull(parameterTypes, "Parameter types of constructor to find cannot be null.");

		try {
			Constructor<T> constructor = clazz.getDeclaredConstructor(parameterTypes);
			constructor.setAccessible(true);
			return constructor;
		} catch (final NoSuchMethodException e) {
			final StringBuilder desc = new StringBuilder();
			desc.append(clazz.getSimpleName());

			StringJoiner joiner = new StringJoiner(", ", "(", ")");

			for (Class<?> type : parameterTypes) {
				joiner.add(type.getSimpleName());
			}

			desc.append(joiner);

			throw new UnknownConstructorException("Could not find constructor '" + desc + "' in " + clazz);
		}
	}

	/**
	 * Finds a field with the specified name in the given class and makes it accessible.
	 * Note: For performance, store the returned value and avoid calling this repeatedly.
	 *
	 * <p>Throws an exception if the field was not found.</p>
	 *
	 * @param clazz     The class to find the field on.
	 * @param fieldName The intermediary (unmapped) name of the field to find (e.g. "field_1817").
	 * @param <T>       The type.
	 * @return The constructor with the specified parameters in the given class.
	 * @throws NullPointerException       If {@code clazz} is null.
	 * @throws NullPointerException       If {@code fieldName} is null.
	 * @throws IllegalArgumentException   If {@code fieldName} is empty.
	 * @throws UnableToFindFieldException If the field could not be found.
	 */
	@Nonnull
	public static <T> Field findField(@Nonnull final Class<? super T> clazz, @Nonnull final String fieldName) {
		Preconditions.checkNotNull(clazz, "Class to find field on cannot be null.");
		Preconditions.checkNotNull(fieldName, "Name of field to find cannot be null.");
		Preconditions.checkArgument(!fieldName.isEmpty(), "Name of field to find cannot be empty.");

		try {
			String name = PatchworkMappingResolver.remapName(INameMappingService.Domain.FIELD, clazz, fieldName);
			Field field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			return field;
		} catch (Exception e) {
			throw new UnableToFindFieldException(e);
		}
	}

	/**
	 * Finds a field with the specified index in the given class and makes it accessible.
	 * Note: For performance, store the returned value and avoid calling this repeatedly.
	 *
	 * <p>Throws an exception if the field is not found.</p>
	 *
	 * @param clazz      The class to find the field on.
	 * @param fieldIndex The index of the field on the class
	 * @param <T>        The type.
	 * @return The constructor with the specified parameters in the given class.
	 * @throws NullPointerException       If {@code clazz} is null.
	 * @throws UnableToFindFieldException If the field could not be found.
	 * @deprecated Use {@link #findField(Class, String)} because field indices change a lot more often than field names do.
	 * TODO: Remove in 1.15
	 */
	@Deprecated
	@Nonnull
	public static <T> Field findField(final Class<? super T> clazz, final int fieldIndex) {
		Preconditions.checkNotNull(clazz, "Class to find field on cannot be null.");

		try {
			final Field f = clazz.getDeclaredFields()[fieldIndex];
			f.setAccessible(true);
			return f;
		} catch (Exception e) {
			throw new UnableToFindFieldException(e);
		}
	}

	public static class UnableToAccessFieldException extends RuntimeException {
		private UnableToAccessFieldException(Exception e) {
			super(e);
		}
	}

	public static class UnableToFindFieldException extends RuntimeException {
		private UnableToFindFieldException(Exception e) {
			super(e);
		}
	}

	public static class UnableToFindMethodException extends RuntimeException {
		public UnableToFindMethodException(Throwable failed) {
			super(failed);
		}
	}

	public static class UnknownConstructorException extends RuntimeException {
		public UnknownConstructorException(final String message) {
			super(message);
		}
	}
}