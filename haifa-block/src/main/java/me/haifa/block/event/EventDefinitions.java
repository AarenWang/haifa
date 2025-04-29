package me.haifa.block.event;

import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Uint;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.abi.datatypes.Event;
import java.util.Arrays;

public class EventDefinitions {
    public static final Event TRANSFER_EVENT = new Event("Transfer",
            Arrays.asList(
                    new TypeReference<Address>(true) {},   // indexed from
                    new TypeReference<Address>(true) {},   // indexed to
                    new TypeReference<Uint256>(false) {}   // value
            )
    );
}

