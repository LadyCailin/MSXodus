package com.methodscript.msxodus;

import com.laytonsmith.PureUtilities.ClassLoading.ClassDiscovery;
import com.laytonsmith.PureUtilities.Common.StreamUtils;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.annotations.api;
import com.laytonsmith.annotations.typeof;
import com.laytonsmith.core.ArgumentValidation;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CByteArray;
import com.laytonsmith.core.constructs.CClassType;
import com.laytonsmith.core.constructs.CClosure;
import com.laytonsmith.core.constructs.CNull;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.environments.GlobalEnv;
import com.laytonsmith.core.exceptions.CRE.CREException;
import com.laytonsmith.core.exceptions.CRE.CREIOException;
import com.laytonsmith.core.exceptions.CRE.CREThrowable;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.exceptions.ProgramFlowManipulationException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.natives.interfaces.Mixed;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import jetbrains.exodus.ExodusException;
import jetbrains.exodus.entitystore.Entity;
import jetbrains.exodus.entitystore.EntityIterable;
import jetbrains.exodus.entitystore.PersistentEntityStore;
import jetbrains.exodus.entitystore.PersistentEntityStores;
import jetbrains.exodus.entitystore.StoreTransaction;
import jetbrains.exodus.entitystore.StoreTransactionalComputable;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;


/**
 *
 */
public class Xodus {

	@typeof("ms.xodus.XodusTransactionException")
	public static class CRENoXodusTransactionException extends CREException {

		@SuppressWarnings("FieldNameHidesFieldInSuperclass")
		public static final CClassType TYPE = CClassType.get(CRENoXodusTransactionException.class);

		public CRENoXodusTransactionException(String msg, Target t) {
			super(msg, t);
		}

		public CRENoXodusTransactionException(String msg, Target t, Throwable th) {
			super(msg, t, th);
		}


		@Override
		public String docs() {
			return "This exception is thrown if an function requiring an xodus transaction is called"
					+ " outside of an exception.";
		}

		@Override
		public Version since() {
			return MSXodus.v1_0_0;
		}

		@Override
		public CClassType[] getSuperclasses() {
			return super.getSuperclasses();
		}

		@Override
		public CClassType[] getInterfaces() {
			return super.getInterfaces();
		}

	}

	@typeof("ms.xodus.XodusException")
	public static class CREXodusException extends CREException {

		@SuppressWarnings("FieldNameHidesFieldInSuperclass")
		public static final CClassType TYPE = CClassType.get(CREXodusException.class);

		public CREXodusException(String msg, Target t) {
			super(msg, t);
		}

		public CREXodusException(String msg, Target t, Throwable th) {
			super(msg, t, th);
		}


		@Override
		public String docs() {
			return "This exception is thrown if a generic exception occurs in Xodus.";
		}

		@Override
		public Version since() {
			return MSXodus.v1_0_0;
		}

		@Override
		public CClassType[] getSuperclasses() {
			return super.getSuperclasses();
		}

		@Override
		public CClassType[] getInterfaces() {
			return super.getInterfaces();
		}

	}

	private static final String XODUS_ENV = "xodus_environment_store_transaction";

	private static StoreTransaction GetTransactionOrFail(Environment env, Target t) {
		StoreTransaction txn = (StoreTransaction)
				env.getEnv(GlobalEnv.class).GetCustom(XODUS_ENV);
		if(txn == null) {
			throw new CRENoXodusTransactionException("Cannot operate on an Xodus database outside of a"
					+ " transaction, call an xodus_transaction_* function first.", t);
		}
		return txn;
	}

	public static CArray EntityToArray(Entity entity) {
		CArray ret = CArray.GetAssociativeArray(Target.UNKNOWN);
		CArray properties = CArray.GetAssociativeArray(Target.UNKNOWN);
		for(String prop : entity.getPropertyNames()) {
			Comparable p = entity.getProperty(prop);
			properties.set(prop, p == null ? CNull.NULL : new CString(p.toString(), Target.UNKNOWN), Target.UNKNOWN);
		}
		CArray links = new CArray(Target.UNKNOWN);
		for(String link : entity.getLinkNames()) {
			links.push(new CString(link, Target.UNKNOWN), Target.UNKNOWN);
		}

		CArray blobs = new CArray(Target.UNKNOWN);
		for(String blob : entity.getBlobNames()) {
			blobs.push(new CString(blob, Target.UNKNOWN), Target.UNKNOWN);
		}

		ret.set("properties", properties, Target.UNKNOWN);
		ret.set("links", links, Target.UNKNOWN);
		ret.set("id", entity.toIdString());
		ret.set("blobs", blobs, Target.UNKNOWN);
		return ret;
	}

	public static Entity EntityFromArray(StoreTransaction trans, CArray entity, Target t) {
		return trans.getEntity(trans.toEntityId(entity.get("id", t).val()));
	}

	public static Entity EntityFromId(StoreTransaction trans, String id) {
		return trans.getEntity(trans.toEntityId(id));
	}

	public static Entity EntityFromMixed(StoreTransaction trans, Mixed entity, Target t) {
		if(entity instanceof CArray) {
			return EntityFromArray(trans, ((CArray) entity), t);
		} else {
			return EntityFromId(trans, entity.val());
		}
	}

	public static String docs() {
		return "Provides methods for manipulating an Xodus database.";
	}

	@api
	public static class xodus_transaction_entity extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{};
		}

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return null;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			File xodusEnvironment = new File(args[0].val());
			String storeName = args[1].val();
			CClosure callback = ArgumentValidation.getObject(args[2], t, CClosure.class);
			boolean readOnly = false;
			if(args.length > 3) {
				readOnly = ArgumentValidation.getBooleanObject(args[3], t);
			}
			if(!xodusEnvironment.isDirectory()) {
				throw new CREXodusException("environment must be a directory, \""
						+ xodusEnvironment.getAbsolutePath() + "\" is not a directory.", t);
			}
			ClassLoader original = Thread.currentThread().getContextClassLoader();

			Thread.currentThread().setContextClassLoader(ClassDiscovery.getDefaultInstance().getDefaultClassLoader());
			try (jetbrains.exodus.env.Environment env
					= Environments.newInstance(xodusEnvironment, new EnvironmentConfig()
											.setEnvIsReadonly(readOnly));
				PersistentEntityStore entityStore
					= PersistentEntityStores.newInstance(env, storeName)) {
				StoreTransactionalComputable<Mixed> exe = new StoreTransactionalComputable<Mixed>() {
					@Override
					public Mixed compute(StoreTransaction txn) {
						environment.getEnv(GlobalEnv.class).SetCustom(XODUS_ENV, txn);
						try {
							return callback.executeCallable(environment, t);
						} catch (ConfigRuntimeException e){
							ConfigRuntimeException.HandleUncaughtException(e, environment);
						} catch (ProgramFlowManipulationException e){
							// Ignored
						} finally {
							environment.getEnv(GlobalEnv.class).SetCustom(XODUS_ENV, null);
						}
						return CNull.NULL;
					}
				};

				if(readOnly) {
					entityStore.computeInReadonlyTransaction(exe);
				} else {
					entityStore.computeInTransaction(exe);
				}

			} catch (ExodusException ex) {
				throw new CREXodusException(ex.getMessage(), t, ex);
			} finally {
				Thread.currentThread().setContextClassLoader(original);
			}
			return CVoid.VOID;
		}

		@Override
		public String getName() {
			return "xodus_transaction_entity";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{3, 4};
		}

		@Override
		public String docs() {
			return "void {environment, callback, [readOnly=false]} Starts a transaction. The callback can then call"
					+ " one or more other methods within a transaction to manipulate the entity.";
		}

		@Override
		public Version since() {
			return MSXodus.v1_0_0;
		}

	}

	@api
	public static class xodus_get_types extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{};
		}

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return null;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			StoreTransaction txn = GetTransactionOrFail(environment, t);
			CArray types = new CArray(t);
			for(String type : txn.getEntityTypes()) {
				types.push(new CString(type, t), t);
			}
			return types;
		}

		@Override
		public String getName() {
			return "xodus_get_types";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{0};
		}

		@Override
		public String docs() {
			return "array {} Returns a list of types in the database.";
		}

		@Override
		public Version since() {
			return MSXodus.v1_0_0;
		}

	}

	@api
	public static class xodus_get_all extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{};
		}

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return null;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			StoreTransaction txn = GetTransactionOrFail(environment, t);
			String type = args[0].val();
			CArray entities = new CArray(t);
			EntityIterable entityIterator = txn.getAll(type);
			for(Entity entity : entityIterator) {
				entities.push(EntityToArray(entity), t);
			}
			return entities;
		}

		@Override
		public String getName() {
			return "xodus_get_all";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "array {type} Returns all entities of a particular type.";
		}

		@Override
		public Version since() {
			return MSXodus.v1_0_0;
		}

	}

	@api
	public static class xodus_entity_from_id extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{};
		}

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return null;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			StoreTransaction st = GetTransactionOrFail(environment, t);
			String id = args[0].val();
			Entity e = st.getEntity(st.toEntityId(id));
			return EntityToArray(e);
		}

		@Override
		public String getName() {
			return "xodus_entity_from_id";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		@Override
		public String docs() {
			return "array {entityId} Given an entity id string, returns the specified entity. The id reference may"
					+ " be obtained from a previous lookup, or for instance the links specified in the object.";
		}

		@Override
		public Version since() {
			return MSXodus.v1_0_0;
		}

	}

	@api
	public static class xodus_read_links extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{};
		}

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return null;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			StoreTransaction st = GetTransactionOrFail(environment, t);
			Entity en = EntityFromMixed(st, args[0], t);
			String link = args[1].val();

			CArray ret = new CArray(t);
			for(Entity e : en.getLinks(link)) {
				ret.push(new CString(e.toIdString(), t), t);
			}
			return ret;
		}

		@Override
		public String getName() {
			return "xodus_read_links";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "array {entity, linkName} Given the entity, returns the ids for the given links, which can"
					+ " then be individually looked up if necessary. The entity may be the entire entity, or just"
					+ " the string id.";
		}

		@Override
		public Version since() {
			return MSXodus.v1_0_0;
		}

	}

	@api
	public static class xodus_read_blob extends AbstractFunction {

		@Override
		public Class<? extends CREThrowable>[] thrown() {
			return new Class[]{CREIOException.class};
		}

		@Override
		public boolean isRestricted() {
			return true;
		}

		@Override
		public Boolean runAsync() {
			return null;
		}

		@Override
		public Mixed exec(Target t, Environment environment, Mixed... args) throws ConfigRuntimeException {
			try {
				StoreTransaction st = GetTransactionOrFail(environment, t);
				Entity en = EntityFromMixed(st, args[0], t);
				String blob = args[1].val();
				InputStream is = en.getBlob(blob);
				if(is == null) {
					return CNull.NULL;
				}
				byte[] b = StreamUtils.GetBytes(is);
				CByteArray ret = CByteArray.wrap(b, t);
				return ret;
			} catch (IOException ex) {
				throw new CREIOException(ex.getMessage(), t, ex);
			}
		}

		@Override
		public String getName() {
			return "xodus_read_blob";
		}

		@Override
		public Integer[] numArgs() {
			return new Integer[]{2};
		}

		@Override
		public String docs() {
			return "byte_array {entity, blobName} Returns the blob with the given name. The entity may be the entire"
					+ " entity, or just the string id.";
		}

		@Override
		public Version since() {
			return MSXodus.v1_0_0;
		}

	}


}
