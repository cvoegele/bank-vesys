package bank.graphql.client.models;

/**
 * Base Object for JSON Parsing this Object represents the most outer brackets {}
 * I dont really like this approach because it is messy. But interpreting the JSON as a String was even worse.
 */
public class Root {
    //data object should never be null but always filled differently
    public Data data;
}

