// Copyright (c) 2003-present, Jodd Team (http://jodd.org)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//
// 1. Redistributions of source code must retain the above copyright notice,
// this list of conditions and the following disclaimer.
//
// 2. Redistributions in binary form must reproduce the above copyright
// notice, this list of conditions and the following disclaimer in the
// documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
// LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
// CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
// SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
// INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.

package jodd.db.oom.dao;

import jodd.bean.BeanUtil;
import jodd.db.DbQuery;
import jodd.db.JoddDb;
import jodd.db.oom.DbEntityDescriptor;
import jodd.db.oom.DbEntityManager;
import jodd.db.oom.DbOomException;
import jodd.db.oom.sqlgen.DbEntitySql;

import java.util.Collection;
import java.util.List;

import static jodd.db.oom.DbOomQuery.query;
import static jodd.db.oom.sqlgen.DbEntitySql.findByColumn;
import static jodd.db.oom.sqlgen.DbEntitySql.insert;

/**
 * Generic DAO. Contains many convenient wrappers.
 */
public class GenericDao {

	// ---------------------------------------------------------------- config

	protected boolean keysGeneratedByDatabase = true;

	/**
	 * Returns <code>true</code> if keys are auto-generated by database.
	 * Otherwise, keys are generated manually.
	 */
	public boolean isKeysGeneratedByDatabase() {
		return keysGeneratedByDatabase;
	}

	/**
	 * Specifies how primary keys are generated.
	 */
	public void setKeysGeneratedByDatabase(final boolean keysGeneratedByDatabase) {
		this.keysGeneratedByDatabase = keysGeneratedByDatabase;
	}

	// ---------------------------------------------------------------- store

	/**
	 * Returns <code>true</code> if entity is persistent.
	 */
	protected <E> boolean isPersistent(final DbEntityDescriptor<E> ded, final E entity) {
		Object key = ded.getIdValue(entity);

		if (key == null) {
			return false;
		}
		if (key instanceof Number) {
			long value = ((Number)key).longValue();

			if (value == 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Sets new ID value for entity.
	 */
	protected <E> void setEntityId(final DbEntityDescriptor<E> ded, final E entity, final long newValue) {
		ded.setIdValue(entity, Long.valueOf(newValue));
	}

	/**
	 * Generates next id for given type.
	 */
	protected long generateNextId(final DbEntityDescriptor ded) {
		throw new UnsupportedOperationException("Use Joy");
	}

	/**
	 * Saves or updates entity. If ID is not <code>null</code>, entity will be updated.
	 * Otherwise, entity will be inserted into the database.
	 */
	public <E> E store(final E entity) {
		DbEntityManager dboom = JoddDb.defaults().getDbEntityManager();
		Class type = entity.getClass();
		DbEntityDescriptor ded = dboom.lookupType(type);

		if (ded == null) {
			throw new DbOomException("Not an entity: " + type);
		}
		if (!isPersistent(ded, entity)) {
			DbQuery q;
			if (keysGeneratedByDatabase) {
				q = query(insert(entity));
				q.setGeneratedKey();
				q.executeUpdate();
				long nextId = q.getGeneratedKey();
				setEntityId(ded, entity, nextId);
			} else {
				long nextId = generateNextId(ded);
				setEntityId(ded, entity, nextId);
				q = query(insert(entity));
				q.executeUpdate();
			}
			q.close();
		} else {
			query(DbEntitySql.updateAll(entity)).autoClose().executeUpdate();
		}
		return entity;
	}

	/**
	 * Simply inserts object into the database.
	 */
	public void save(final Object entity) {
		DbQuery q = query(insert(entity));
		q.autoClose().executeUpdate();
	}

	/**
	 * Inserts bunch of objects into the database.
	 * @see #save(Object)
	 */
	public void saveAll(final Collection entities) {
		for (Object entity: entities) {
			save(entity);
		}
	}

	// ---------------------------------------------------------------- update

	/**
	 * Updates single entity.
	 */
	public void update(final Object entity) {
		query(DbEntitySql.updateAll(entity)).autoClose().executeUpdate();
	}

	/**
	 * Updates all entities.
	 * @see #update(Object)
	 */
	public void updateAll(final Collection entities) {
		for (Object entity : entities) {
			update(entity);
		}
	}

	/**
	 * Updates single property in database and in the bean.
	 */
	public <E> E updateProperty(final E entity, final String name, final Object newValue) {
		query(DbEntitySql.updateColumn(entity, name, newValue)).autoClose().executeUpdate();
		BeanUtil.declared.setProperty(entity, name, newValue);
		return entity;
	}

	/**
	 * Updates property in the database by storing the current property value.
	 */
	public <E> E updateProperty(final E entity, final String name) {
		Object value = BeanUtil.declared.getProperty(entity, name);
		query(DbEntitySql.updateColumn(entity, name, value)).autoClose().executeUpdate();
		return entity;
	}

	// ---------------------------------------------------------------- find

	/**
	 * Finds single entity by its id.
	 */
	public <E> E findById(final Class<E> entityType, final long id) {
		return query(DbEntitySql.findById(entityType, id)).autoClose().find(entityType);
	}

	/**
	 * Finds single entity by matching property.
	 */
	public <E> E findOneByProperty(final Class<E> entityType, final String name, final Object value) {
		return query(findByColumn(entityType, name, value)).autoClose().find(entityType);
	}

	/**
	 * Finds one entity for given criteria.
	 */
	@SuppressWarnings({"unchecked"})
	public <E> E findOne(final Object criteria) {
		return (E) query(DbEntitySql.find(criteria)).autoClose().find(criteria.getClass());
	}

	/**
	 * Finds list of entities matching given criteria.
	 */
	@SuppressWarnings({"unchecked"})
	public <E> List<E> find(final Object criteria) {
		return query(DbEntitySql.find(criteria)).autoClose().list(criteria.getClass());
	}

	/**
	 * Finds list of entities matching given criteria.
	 */
	public <E> List<E> find(final Class<E> entityType, final Object criteria) {
		return query(DbEntitySql.find(entityType, criteria)).autoClose().list(entityType);
	}

	// ---------------------------------------------------------------- delete

	/**
	 * Deleted single entity by its id.
	 */
	public void deleteById(final Class entityType, final long id) {
		query(DbEntitySql.deleteById(entityType, id)).autoClose().executeUpdate();
	}

	/**
	 * Delete single object by its id. Resets ID value.
	 */
	public void deleteById(final Object entity) {
		if (entity != null) {
			int result = query(DbEntitySql.deleteById(entity)).autoClose().executeUpdate();

			if (result != 0) {
				// now reset the ID value
				DbEntityManager dboom = JoddDb.defaults().getDbEntityManager();
				Class type = entity.getClass();
				DbEntityDescriptor ded = dboom.lookupType(type);

				setEntityId(ded, entity, 0);
			}
		}
	}

	/**
	 * Deletes all objects by their id.
	 */
	public void deleteAllById(final Collection objects) {
		for (Object entity : objects) {
			deleteById(entity);
		}
	}

	// ---------------------------------------------------------------- count

	/**
	 * Counts number of all entities.
	 */
	public long count(final Class entityType) {
		return query(DbEntitySql.count(entityType)).autoClose().executeCount();
	}


	// ---------------------------------------------------------------- increase

	/**
	 * Increases a property.
	 */
	public void increaseProperty(final Class entityType, final long id, final String name, final Number delta) {
		query(DbEntitySql.increaseColumn(entityType, id, name, delta, true)).autoClose().executeUpdate();
	}

	/**
	 * Decreases a property.
	 */
	public void decreaseProperty(final Class entityType, final long id, final String name, final Number delta) {
		query(DbEntitySql.increaseColumn(entityType, id, name, delta, false)).autoClose().executeUpdate();
	}

	// ---------------------------------------------------------------- related

	/**
	 * Finds related entity.
	 */
	public <E> List<E> findRelated(final Class<E> target, final Object source) {
		return query(DbEntitySql.findForeign(target, source)).autoClose().list(target);
	}

	// ---------------------------------------------------------------- list

	/**
	 * List all entities.
	 */
	public <E> List<E> listAll(final Class<E> target) {
		return query(DbEntitySql.from(target)).autoClose().list(target);
	}

}