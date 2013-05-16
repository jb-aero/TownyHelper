# TownyHelper

A Towny-hooking extension for CommandHelper

## Functions

### towny_towns
```
Returns: array
Arguments: 
Description: Returns an array where the keys are town names and the values are
 arrays with keys mayor(string), assistants(array of strings), residents(array of strings),
 nation(string or null), spawn(locationArray or null)
```

### towny_canbuild
```
Returns: boolean
Arguments: player, location
Description: Checks if a player has build permissions at a location.
```

### towny_canbreak
```
Returns: boolean
Arguments: player, location
Description: Checks if a player has destroy permissions at a location.
```
