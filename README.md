# Functions
## Xodus
Provides methods for manipulating an Xodus database.

### array xodus\_entity\_from\_id(entityId):
Given an entity id string, returns the specified entity. The id reference may be obtained from a previous lookup, or for instance the links specified in the object.

### array xodus\_get\_all(type):
Returns all entities of a particular type.

### array xodus\_get\_types():
Returns a list of types in the database.

### byte\_array xodus\_read\_blob(entity, blobName):
Returns the blob with the given name. The entity may be the entire entity, or just the string id.

### array xodus\_read\_links(entity, linkName):
Given the entity, returns the ids for the given links, which can then be individually looked up if necessary. The entity may be the entire entity, or just the string id.

### void xodus\_transaction\_entity(environment, callback, [readOnly=false]):
Starts a transaction. The callback can then call one or more other methods within a transaction to manipulate the entity.
