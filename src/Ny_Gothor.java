/* TITLE: Ny_Gothor
 AUTHOR: Oliver Staddon
 DATE: 18/11/2024
 VERSION: 1
 DESCRIPTION:
    A text-based adventure game where the player navigates through a series of caverns, of which are procedurally 
    generated, collecting items and defeating monsters. The goal, escape the cave.
 */ 

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Ny_Gothor {
    public static void main(String[] args) throws Exception {
        mainMenu();  
    } // END main

    //#region Main menu

    // Method for the main menu
    public static void mainMenu() throws IOException, ClassNotFoundException{
        System.out.println("Welcome to Ny'Gothor");

        int choice = 0;

        // Loop while the user has not quit
        while(choice != 4){
            System.out.println("1. New game");
            System.out.println("2. Load game");
            System.out.println("3. Help");
            System.out.println("4. Quit game");
    
            choice = getChoiceAsInt("");
    
            if(choice == 1){
                newGame();
            } else if(choice == 2){

                // Get game saves from folder directory
                Path saveName = getGameSaves("Ny-Gothor Saves");

                // If the save exists
                if(saveName != null){
                    player player = loadGame(saveName); // Get player data from save file
                    gameLoop(player); // Enter game loop with loaded player data
                }
            } else if(choice == 3){
                help();
            } else if(choice == 4){
                return; // Closes application
            }
        }        

        return;
    } // END mainMenu


    // Metbhod to start a new game
    public static void newGame() throws IOException{
        
        // Seperates in terminal and displays introduction text
        System.out.println("----------------------------------------------------------------------");
        introduction();

        // Initialise the game and start the game loop
        player player = initialiseGame();
        gameLoop(player);

        return;
    } // END newGame

    
    // Simple method that outputs key game elements
    public static void help(){
        System.out.println("----------------------------------------------------------------------");
        System.out.println("Help:");
        System.out.println("Input room numbers as shown in game to visit them.");
        System.out.println("Input item name to use it in combat.");
        System.out.println("Type 'SAVE' when in a room to save the game.");
        System.out.println("Type 'Items' to show owned items");
        System.out.println("----------------------------------------------------------------------");

        return;
    } // END help

    //#endregion



    

    //#region Saving

    // Method to save the game
    public static void saveGame(player player, String folderName, String saveName) throws IOException{

        // Create path for the save directory (in users/userName/Documents)
        Path savepath = Paths.get(System.getProperty("user.home"), "Documents", folderName);
        
        // Create directory if it doesn't exist already
        Files.createDirectories(savepath);

        // Resolve file path, allowing for output streams (eg. format to /home/user/Documents/Ny-Gothor/game1.save)
        Path filePath = savepath.resolve(saveName);

        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath.toFile()));
        out.writeObject(player); // Write the player record to the stream (serialise to file)
        
        return;
    } // END saveGame


    // Method to load save game from the file path
    public static player loadGame(Path filePath) throws IOException, ClassNotFoundException{

        // Create an object reader and convert the path to a file, open stream to read from file
        ObjectInputStream in = new ObjectInputStream(new FileInputStream(filePath.toFile()));
            
        // Read from the file path and cast to type player to allow for input into game loop
        return (player)in.readObject(); 

    } // END loadGame


    // Method to output game saves
    public static Path getGameSaves(String folderPath) throws IOException{
        
        // Create path to directory
        Path savePath = Paths.get(System.getProperty("user.home"), "Documents", folderPath);

        // Ensure directory exists
        if(!Files.exists(savePath) || !Files.isDirectory(savePath)){
            System.out.println("Save directory not found.");
            return null;
        }

        // Get a list of files in directory
        // Filter to only include files and no directories, convert to array of path objects (:: is method reference operator)
        Path[] saveFiles = Files.list(savePath).filter(Files::isRegularFile).toArray(Path[]::new);

        // Check for save files
        if(saveFiles.length == 0){
            System.out.println("No saves found");
            return null;
        }

        // Output availble save files in form "i: saveName"
        System.out.println("Available save files:");
        for(int i = 0; i < saveFiles.length; i++){
            System.out.println((i + 1) + ": " + saveFiles[i].getFileName());
        }

        int choice = getChoiceAsInt("");

        // Ensure input is correct and return
        if(choice > 0 && choice <= saveFiles.length){
            return saveFiles[choice - 1];
        }else{
            System.out.println("Invalid choice.");
            return null;
        }
        
    } // END getGameSaves

    //#endregion





    //#region Game

    // Method that handles the main loop of the game
    public static void gameLoop(player player) throws IOException{
        boolean isPlayerAtEnd = false;

        // Loop while player is not dead
        while(!isPlayerDead(player) && !isPlayerAtEnd){

            // Get current room and item in room
            room currentRoom = getPlayerRoomList(player)[getPlayerCurrentRoomIndex(player)];

            typeMessage(getRoomDescription(currentRoom), 10);

            // If there is a monster in the room
            if(roomHasMonster(currentRoom)){
                monsterEncountered(player, getRoomMonster(currentRoom));
            } 

            // If player is fully insane
            if(getPlayerSanity(player) >= 100){
                insane();
            }

            // If there is an item
            if(getRoomItem(currentRoom) != null){
                outputRoomItem(player);
                setItemTaken(currentRoom, roomItemTaken(player)); // Update room item status
            }

            // Output path choices and go to chosen room
            String roomInput = outputRoomChoices(player);
            if(roomInput.equals("SAVE")){
                String saveName = getChoiceAsString("Enter save name.");
                saveGame(player, "Ny-Gothor Saves", saveName);
            }else if(roomInput.equals("Items")){
                outputOwnedItems(player);
            }else if(roomInput.matches("-?\\d+")){
                int chosenRoom = Integer.parseInt(roomInput);

                boolean validRoom = false; // Flag to track if a valid room is found

                // Check if input matches an index that the room connects to and go to it
                int[] linkedIndicies = getRoomIndexArray(currentRoom);
                for(int i = 0; i < linkedIndicies.length; i++){
                    int currentIndex = linkedIndicies[i];
                    if(chosenRoom == currentIndex){
                        validRoom = true;
                        setPlayerCurrentRoomIndex(player, goToRoom(player, currentRoom, chosenRoom));
                    }
                }

                if(chosenRoom == -1){
                    validRoom = true;
                    returnToLastRoom(player);
                }

                if(!validRoom){
                    System.out.println("Input does not match available choices.");
                    System.out.println("----------------------------------------------------------------------");
                }
            }else{
                System.out.println("Input does not match available choices.");
                System.out.println("----------------------------------------------------------------------");
            }
        }

        // Display death if player is dead
        if(isPlayerDead(player)){
            death();
            return;
        }

        // If the player is at the final room
        if(isPlayerAtEnd){
            nyGothor(player); // Ouput story
            if(isIncantationSpoken(player)){
                monster nyGothor = initialiseMonster(new monster(), 1000, 70, "Ny-Gothor");
                monsterEncountered(player, nyGothor);
                
                // If player killed ny gothor
                if(getMonsterHealth(nyGothor) == 0){
                    ending(player);
                }
            }else if(!isIncantationSpoken(player)){
                death();
            }
        }

        return;
    } // END gameLoop


    // Handles when player goes insane
    public static void insane(){

        typeMessage("Your weak mind cannot comprehend these creatures as you fall into insanity", 50);
        death();

        return;
    } // END insane


    // Display death message
    public static void death(){

        System.out.println("----------------------------------------------------------------------");
        typeMessage("You are dead.", 50);

        return;
    } // END death


    //#region Room navigation

    // Takes current room and decision and go to the next room 
    public static int goToRoom(player player, room currentRoom, int roomChoice){

        int chosenRoomIndex = -1;

        // Loop through the rooms index array of other rooms
        for(int i = 0; i < getRoomIndexArray(currentRoom).length; i++){
                
            // If the current index is equal to the choice
            if(getRoomIndexArray(currentRoom)[i] == roomChoice){

                // Add the current room index to stack for potential backtracking
                getPlayerPrevRooms(player).push(getRoomIndex(currentRoom));
                
                // Set the chosen index to the value at the index-1
                return chosenRoomIndex = getRoomIndexArray(currentRoom)[i];
            }
        }

        // Return -1 if choice not found
        return chosenRoomIndex;
    } // END goToRoom


    // Takes current room and goes to previous room
    public static void returnToLastRoom(player player){

        // Do nothing if there are no other rooms
        if(getPlayerPrevRooms(player).isEmpty()){
            return;
        }

        // Update the current room index
        setPlayerCurrentRoomIndex(player, getPlayerPrevRooms(player).get(getPlayerPrevRooms(player).size() - 1));
        
        // Remove the last room only if there is more than one previous rooms
        if(getPlayerPrevRooms(player).size() > 1){
            getPlayerPrevRooms(player).pop();
        }

        return;
    } // END returnToLastRoom

    //#endregion

    //#endregion





    //#region Combat

    // Method that handles a monster encounter, has MANY sub methods :3
    public static void monsterEncountered(player player, monster monster){
        int choice;
        boolean isRunning = false;
        final int TEXT_SPEED = 50;
        
        room currentRoom = getPlayerRoomList(player)[getPlayerCurrentRoomIndex(player)];

        // Output start of encounter and decrease sanity (check if monster is ny-gothor for different output)
        if(getMonsterName(monster).equals("Ny-Gothor")){
            typeMessage("At the sight of " + getMonsterName(monster) + " you feel your mind falter.", TEXT_SPEED);
            typeMessage("Sanity decreased by " + getMonsterSanityImpact(monster), TEXT_SPEED);
        }else{
            typeMessage("You notice something shifting within the room.", TEXT_SPEED);
            typeMessage("At the sight of " + getMonsterName(monster) + " you feel your mind falter.", TEXT_SPEED);
            typeMessage("Sanity decreased by " + getMonsterSanityImpact(monster), TEXT_SPEED);
        }        
        
        // Update sanity
        setPlayerSanity(player, getPlayerSanity(player) + getMonsterSanityImpact(monster));

        // Loop while the monster is alive and the player is not dead or player has run away
        while(getMonsterHealth(monster) > 0 && !isPlayerDead(player) && !isRunning){

            // Output status
            typeMessage(getMonsterName(monster) + " has " + getMonsterHealth(monster) + " health remaining.", TEXT_SPEED);
            typeMessage("You have " + getPlayerHealth(player) + " health remaining. You are " + getPlayerSanity(player) + "% insane.", TEXT_SPEED);

            choice = combatChoices();
            if(choice == 1){
                fightMonster(player, monster);
            }else if (choice == 2){
                isRunning = true;
            }
        }

        // If the player runs, return to last room
        if(isRunning){
            typeMessage("You flee.", TEXT_SPEED);
            returnToLastRoom(player);
            return;
        }

        // If the monster was killed, the room doesn't have one anymore
        if(isMonsterDead(monster)){
            typeMessage("The beast falls.", TEXT_SPEED);
            setRoomHasMonster(currentRoom, false);
        }

        return;
    } // END monsterCombat

    
    // Outputs choices when in combat, returns choice. Ensures correct input
    public static int combatChoices(){
        final int TEXT_SPEED = 50;
        int choice = 0;
        boolean inputValid = false;
 
        String message1 = "Fight: 1";
        String message2 = "Run: 2";

        // Loop until valid input
        while(inputValid == false){
            
            typeMessage(message1, TEXT_SPEED);
            typeMessage(message2, TEXT_SPEED);
    
            choice = getChoiceAsInt("");

            if(choice == 1 || choice == 2){
                inputValid = true;
            }else{
                System.out.println("Invalid input.");
            }
        }
        return choice;
    } // END combatChoices


    // Method that handles the player and monster attacks
    public static void fightMonster(player player, monster monster){
        final int TEXT_SPEED = 50;

        Random rnd = new Random();

        // Output player items and get choice of weapon
        outputOwnedItems(player);
        items itemChoice = getItemChoice(player);
        
        // If monster did/did not dodge attack
        int monsterDodge = rnd.nextInt(101);
        if(monsterDodge < getMonsterDodgeChance(monster)){
            updateMonsterHealth(monster, getItemDamage(itemChoice)); // Do damage to monster
        }else{
            typeMessage("The attack missed", TEXT_SPEED);
        }

        // End combat if monster killed
        if(getMonsterHealth(monster) <= 0){
            setMonsterDead(monster, true);
            return;
        }

        // Monster turn to attack
        typeMessage("The monster will attack", TEXT_SPEED);

        // If monster did/did not land attack
        int monsterAttack = rnd.nextInt(101);
        if(monsterAttack < getMonsterAttackChance(monster)){
            updatePlayerHealth(player, getMonsterDamage(monster));
        }else{
            typeMessage("The attack missed", TEXT_SPEED);
        }

        // If player has died
        if(getPlayerHealth(player) <= 0){
            setPlayerDead(player, true);
        }

        System.out.println("----------------------------------------------------------------------");

        return;
    } // END fightMonster
    

    // Method to return the choice from the inventory
    public static items getItemChoice(player player){
        Scanner scanner = new Scanner(System.in);
        String input;
        items itemChosen = null;
        boolean isValid = false;

        // Loop until correct input
        while(isValid == false){
            input = getChoiceAsString("What will you use?");

            // Iterate through owned items to see if choice is owned
            for(int i = 0; i < getPlayerInventory(player).length; i++){
                items item = getPlayerInventory(player)[i];

                // If item is owned, assign item
                if(input.equals(getItemName(item))){
                    itemChosen = item;
                    isValid = true;
                }   
            }
            
            // If input is invalid
            if(isValid == false){
                System.out.println("Invalid input. Enter item name.");
            }
        }

        return itemChosen;
    } // END getItemChoice

    //#endregion





    //#region Initialise

    // Initialises the player, calls all other create methods as these are stored within the player
    public static player initialiseGame(){
        
        // Total number of rooms in the game
        final int ROOM_COUNT = 10; 
        
        // Create the player and set values
        player player = new player();
        setPlayerHealth(player, 100);
        setPlayerSanity(player, 0);
        setPlayerDead(player, false);
        setIncantationSpoken(player, false);
        setPlayerCurrentRoomIndex(player, 0);

        // Declare and initalise all items, monsters, and room layout
        declareItems(player);
        declareMonsters(player);
        declareRooms(player, ROOM_COUNT);
        
        return player;
    } // END initialiseGame

    //#region Initialise rooms

    // Declares x amount of rooms and assigns details
    public static void declareRooms(player player, int roomCount){
        
        // Set and get room list
        setPlayerRoomList(player, new room[roomCount]);
        room[] roomList = getPlayerRoomList(player);
        
        // Loops through the amount of rooms in the game (allows differnt sized maps)
        for(int i = 0; i < roomCount; i++){

            // Create a new room at index and set index
            roomList[i] = new room();
            setRoomIndex(roomList[i], i);
        }

        // Set room list and assign indicies
        setPlayerRoomList(player, roomList);
        assignRoomIndex(player, roomCount);

        // Set room details (done in seperate loop as details depend on indicies)
        for(int i = 0; i < roomCount; i++){
            roomList[i] = setRoomDetails(roomList[i], player);
        }

        return;
    } // END declareRooms


    // Method to assign each room with the indicies of the rooms it leads to
    public static void assignRoomIndex(player player, int roomCount){

        // A hashset can be used to track all used indicies as all items are unique
        Set<Integer> globalUsedIndices = new HashSet<>();
        Random rnd = new Random();
        final int MAX_LOOP_ATTEMPTS = 10;

        // Loop through all rooms and set indicies
        for(int i = 0; i < getPlayerRoomList(player).length; i++){
            Set<Integer> usedIndices = new HashSet<>(); // Track used indices for this room
            room currentRoom = getPlayerRoomList(player)[i];
            
            // Get a random number for the amount of connections the room can have (1-3) and initialise
            int connections = rnd.nextInt(3) + 1;
            setRoomIndexArray(currentRoom, new int[connections]);
            int[] roomIndexArray = getRoomIndexArray(currentRoom);

            // Ensure that important rooms have/don't have connection
            if(getRoomIndex(currentRoom) == 0){ 
                connections = 2;
            }else if(getRoomIndex(currentRoom) == 1 || getRoomIndex(currentRoom) == 2){ // Altar room/end room
                connections = 0;
            }

            // Loop through three times (for maximum possible paths)
            for(int x = 0; x < connections; x++){
                            
                // Reset for each path
                boolean foundUniqueIndex = false;
                int loopAttempts = 0;

                while(!foundUniqueIndex && loopAttempts < MAX_LOOP_ATTEMPTS){ // Loop while no unique index
                    int randomIndex = rnd.nextInt(roomCount); // Get a random number for the index within the range of the total rooms

                    // If the current room, and any other room does not contain the index 
                    if(randomIndex != getRoomIndex(currentRoom) && !usedIndices.contains(randomIndex) && !globalUsedIndices.contains(randomIndex)){
                        
                        // Set the current room index at x to the found index
                        roomIndexArray[x] = randomIndex;

                        // Mark the index as used globally and for the room
                        usedIndices.add(randomIndex);
                        globalUsedIndices.add(randomIndex);
                        
                        foundUniqueIndex = true;
                    }
                    else{
                        loopAttempts++;
                    }
                }

                // When no unique index can be found path loops back to start
                if(!foundUniqueIndex){
                    roomIndexArray[x] = 0;
                    setRoomIndexArray(currentRoom, roomIndexArray);
                }
            }
        }
        return;
    } // END assignRoomIndex


    // Method to set the fields within each room
    public static room setRoomDetails(room room, player player){

        Random rnd = new Random();
        final int chanceForMonster = 20;
        final int chanceForItem = 50;

        setItemTaken(room, false);
        setRoomHasMonster(room, false);

        // Random chance for room to have a item unless start room
        if(chanceForItem > rnd.nextInt(101) && getRoomIndex(room) != 0){
            setRoomItem(room, addItemToRoom(player));
        }

        // Random chance for room to have a monster unless start room
        if(chanceForMonster > rnd.nextInt(101) && getRoomIndex(room) != 0){
            setRoomMonster(room, addMonsterToRoom(player));
            setRoomHasMonster(room, true);
        }

        // Assigning room descriptions
        if(getRoomIndex(room) == 0){ // Starter room assigned index 0
            setRoomDescription(room, "The dying light from where you fell shines down.");
            setPathDescription(room, "A gleaming light shines down");
        }else if(getRoomIndex(room) == 1){ // Altar room
            setPathDescription(room, "Whispers echo. Whipsers beckon.");
        }else if(getRoomIndex(room) == 2){ // End room
            setPathDescription(room, "The path seems to trail into infinity with no return.");
        }else{
            setRoomDescription(room, getRndRoomDescription());
            setPathDescription(room, getRndPathDescription());
        }
        return room;
    } // END setRoomsDetails


    // Get a random item and returns it for the room
    public static items addItemToRoom(player player){
        Random rnd = new Random();
        items itemForRoom = null;
        final int MAX_LOOP_ATTEMPTS = 5;
        int loopAttempts = 0;

        // By default items cant be added
        boolean itemCanBeAdded = false;

        // While to add an item, ensures that only one instance of each item exists
        while(!itemCanBeAdded && loopAttempts < MAX_LOOP_ATTEMPTS){
            
            // Get a random item for the room from the list
            items[] allItems = getPlayerAllItems(player);
            itemForRoom = allItems[rnd.nextInt(allItems.length)];

            // If the item does not exist in the room, it can be added
            if(itemExistsInRoom(itemForRoom) == false){
                setItemExistsInRoom(itemForRoom, true);
                itemCanBeAdded = true;
            }else{ // Else item does not exist
                itemForRoom = null;
            }

            loopAttempts++;
        }

        return itemForRoom;
    } // END addItemToRoom


    // Get a random monster and returns it for the room
    public static monster addMonsterToRoom(player player){
        Random rnd = new Random();
        monster monsterForRoom = null;
 
        // Get a random monster for the room from the list
        monster[] monsterList = getPlayerMonsterList(player);
        monsterForRoom = monsterList[rnd.nextInt(monsterList.length)];

        return monsterForRoom;
    } // END addMonsterToRoom


    // Gets a random string for the path description to the room from a pre-defined assortment
    public static String getRndPathDescription(){
        Random rnd = new Random();
        String choice;
        List<String> descriptions = new ArrayList<>();

        // Declare unique strings for path descriptions
        String msg1 = "A path descends into an abyssal gloom, where the walls seem to pulsate with a loathsome, unseen life. Strange symbols writhe faintly upon the stone, mocking your sanity.";
        String msg2 = "A narrow passage coils through the rock, its air heavy with the scent of decay and ancient dust. Faint whispers, carried by no discernible wind, beckon from the unseen recesses.";
        String msg3 = "A corridor of jagged stone, its surface slick with a viscous, black substance that reflects the feeble light. The walls seem to close in, as though alive.";
        String msg4 = "The cavern yawns open, its towering stalactites resembling the fangs of some primordial beast. Shadows dance erratically across the ground, though no flame illuminates the space.";
        String msg5 = "A passage winds in a serpentine manner, constricting like the coils of a serpent. An oppressive silence fills the space, broken only by the faint sound of dripping water far in the unseen depths.";
        String msg6 = "A tunnel twists unnervingly, its walls seeming to ripple like water disturbed by some unseen force. The floor looks wrong, as though it shifts slightly, responding to your presence.";
        String msg7 = "A stifling darkness envelops the corridor ahead, the kind that seems to swallow light whole. Faint, echoing sounds drift through the space - whether the cries of distant explorers or the last gasps of something far more ancient and terrible, you cannot tell.";
        String msg8 = "The path curves sharply here, vanishing into a maw of impenetrable shadow. A faint glow emanates from the stone, though its source is unknown, and the oppressive atmosphere suggests something old.";
        String msg9 = "A low, steady hum resonates through the tunnel, as if the earth itself sings an alien hymn. The walls are covered in strange, phosphorescent fungi that cast an unholy light, illuminating strange patterns in the rock.";

        // Add to list
        descriptions.add(msg1);
        descriptions.add(msg2);
        descriptions.add(msg3);
        descriptions.add(msg4);
        descriptions.add(msg5);
        descriptions.add(msg6);
        descriptions.add(msg7);
        descriptions.add(msg8);
        descriptions.add(msg9);

        // Returns a random string from the list using the length of the lsit        
        choice = descriptions.get(rnd.nextInt(descriptions.size()));

        return choice;
    } // END getRndPathDescription


    // Gets a random string for the room description from a pre-defined assortment
    public static String getRndRoomDescription(){
        Random rnd = new Random();
        String choice;
        List<String> descriptions = new ArrayList<>();

        // Declare unique strings for room descriptions
        String msg1 = "The chamber is vast, its uneven walls appear carved by some long-forgotten, force. A sickly green light emanates from strange, rune-covered stones embedded in the floor. The air is thick with the scent of rot and something far older, as if the room itself is alive.";
        String msg2 = "You find yourself in a low-ceilinged room, the oppressive weight of centuries pressing down from above. Faded murals cover the walls, depicting twisted forms locked in eternal torment, their eyes following you with an unsettling awareness.";
        String msg3 = "The room's walls are slick with moisture, unnaturally cold, as though the very stone rejects the warmth of life. Piles of ancient bones, bleached and brittle, litter the floor.";
        String msg4 = "The chamber opens into a grand, circular space, the ceiling lost in shadows far above. In the centre stands an altar of black stone, its surface etched with unreadable glyphs that seem to pulse faintly in the dim light.";
        String msg5 = "The room is unnervingly symmetrical, every corner too sharp, every line too perfect. The air buzzes with a low, almost imperceptible hum, while a faint vibration runs through the stone floor. ";
        String msg6 = "The space is cramped, suffocating even, with the ceiling sagging as though the weight of countless eons threatens to crush all within. In the corner a mound of strange, decayed fabric lies, its contents shifting ever so slightly.";
        String msg7 = "This room feels wrong, as if it exists in defiance of natural law. The air is dense, thick with a palpable sense of unease. Strange angular shapes cover the walls - symbols that seem to shift and change when viewed out of the corner of your eye. The ceiling drips with a viscous fluid that evaporates before reaching the ground, leaving the room in a constant state of strange anticipation.";
        String msg8 = "A circular pit dominates the centre of the room, surrounded by jagged, unnatural formations that seem to grow from the floor. The pit is impossibly deep, and from within it rises a foul, cloying mist that carries with it whispers - faint and indistinct, but unmistakably filled with fear and despair.";
        String msg9 = "The room is vast, but the oppressive darkness swallows all but the immediate space around you. The ground beneath your feet is uneven, as though the floor is slowly warping under the weight of something beyond human reckoning.";
        String msg10 = "A faint blue light seeps into the room from an unknown source, casting long, distorted shadows across the floor. The walls are etched with strange geometric patterns that seem to lead your eye in circles, drawing you deeper into their maddening design.";

        // Add to list
        descriptions.add(msg1);
        descriptions.add(msg2);
        descriptions.add(msg3);
        descriptions.add(msg4);
        descriptions.add(msg5);
        descriptions.add(msg6);
        descriptions.add(msg7);
        descriptions.add(msg8);
        descriptions.add(msg9);
        descriptions.add(msg10);

        // Returns a random string from the list using the length of the lsit        
        choice = descriptions.get(rnd.nextInt(descriptions.size()));

        return choice;
    } // END getRndRoomDescription

    //#endregion





    //#region Initialise monsters

    // Declares each monster
    public static void declareMonsters(player player){

        setPlayerMonsterList(player, new monster[]{
            initialiseMonster(new monster(), 100, 20, "Ky-Tagar"),
            initialiseMonster(new monster(), 200, 10, "Azakoth"),
            initialiseMonster(new monster(), 20, 40, "Agaroth")
        });

        return;
    } // END declareMonsters


    // Assigns values within the monster record
    public static monster initialiseMonster(monster monster, int health, int damage, String name){
        Random rnd = new Random();

        // Set these stats randomly for variation
        int sanityImpact = rnd.nextInt(20);
        int attackChance = rnd.nextInt(80);
        int dodgeChance = rnd.nextInt(15);

        // Assign values to monster
        setMonsterName(monster, name);
        setMonsterHealth(monster, health);
        setMonsterDamage(monster, damage);
        setMonsterSanityImpact(monster, sanityImpact);
        setMonsterAttackChance(monster, attackChance);
        setMonsterDodgeChance(monster, dodgeChance);
        setMonsterDead(monster, false);

        return monster;
    } // END initialiseMonster

    //#endregion





    //#region Initialise items

    // Declares all items for game and initialises them
    public static void declareItems(player player){
        
        // Create inventory with starter item
        setPlayerInventory(player, new items[]{initialiseItem(new items(),30, "Hatchet")});

        // Items found within the cavern listed below
        setPlayerAllItems(player, new items[]{
            initialiseItem(new items(), 22, "Knife"),
            initialiseItem(new items(), 50, "Club"),
            initialiseItem(new items(), 36, "Spear"),
            initialiseItem(new items(), 60, "Sword"),
            initialiseItem(new items(), 1000, "Dynamite")
        });
        
        return;
    } // END declareItems


    // Method that takes item and details, sets them, and returns the item
    public static items initialiseItem(items item, int damage, String name){

        setItemDamage(item, damage);
        setItemName(item, name);
        setItemExistsInRoom(item, false); // False by default

        return item;
    } // END initialiseItem

    //#endregion

    //#endregion





    //#region Update methods

    public static void addItem(player player, items itemToAdd){
        // Create a new array with one more slot than the existing inventory
        items[] newInventory = new items[getPlayerInventory(player).length + 1];

        // Copy the existing inventory to the new array
        for(int i = 0; i < getPlayerInventory(player).length; i++){
            newInventory[i] = getPlayerInventory(player)[i];
        }

        // Add the new item to the last slot of the new array
        newInventory[newInventory.length - 1] = itemToAdd;

        // Assign the new array to the player inventory
        setPlayerInventory(player, newInventory);

        return;
    } // END addItem


    // Method to update the player health, returns a boolean to indicate death
    public static boolean updatePlayerHealth(player player, int healthChange){

        setPlayerHealth(player, getPlayerHealth(player) - healthChange);

        if(getPlayerHealth(player) <= 0){
            return true; // Player has died
        }

        return false;
    } // END updatePlayerHealth

    
    // Method to update players sanity
    public static void updatePlayerSanity(player player, int sanityChange){

        setPlayerSanity(player, getPlayerSanity(player) + sanityChange);

        return;
    } // END updatePlayerSanity


    // Method to update monster health
    public static boolean updateMonsterHealth(monster monster, int healthChange){

        setMonsterHealth(monster, getMonsterHealth(monster) - healthChange);

        if(getMonsterHealth(monster) <= 0){
            return true; // Monster has died
        }

        return false;
    } // END updateMonsterHealth

    //#endregion
    




    //#region I/O game methods

    // Method to show what items the player currently has
    public static void outputOwnedItems(player player){
        typeMessage("You currently have:", 50);

        items[] inventory = getPlayerInventory(player);
        
        // Loop through all owned items and output name and damage
        for(int i = 0; i < inventory.length; i++){
            items item = inventory[i];
            typeMessage(" - " + getItemName(item) + " -- Damage: " + getItemDamage(item), 50);
        }

        System.out.println("----------------------------------------------------------------------");

        return;
    } // END outputItems


    // Method to output the choices availble in the current room
    public static String outputRoomChoices(player player){
        String chosenRoom;

        // Get the current room
        room currentRoom = getPlayerRoomList(player)[getPlayerCurrentRoomIndex(player)];

        typeMessage("Which path do you take?", 50);

        int[] indexArray = getRoomIndexArray(currentRoom);

        // Loop through the indexArray of room and display the path description of that room
        for(int i = 0; i < indexArray.length; i++){
            int roomIndex = indexArray[i];

            // Get the room associated with the current index
            room nextRoom = getPlayerRoomList(player)[roomIndex];

            typeMessage(roomIndex + ": " + getPathDescription(nextRoom), 10);
        }            

        typeMessage("-1: Return to last room", 10);

        // Gets room input
        chosenRoom = getChoiceAsString("");

        return chosenRoom;
    } // END outputRoomChoices


    // Method to output the choices availble in the current room
    public static void outputRoomItem(player player){

        // Get the current room
        room currentRoom = getPlayerRoomList(player)[getPlayerCurrentRoomIndex(player)];
        typeMessage("Within this room you notice a " + getItemName(getRoomItem(currentRoom)), 50);

        return;
    } // END outputRoomItem


    // Method to output the choices availble in the current room
    public static boolean roomItemTaken(player player){
        
        boolean itemTaken = false;
        room currentRoom = getPlayerRoomList(player)[getPlayerCurrentRoomIndex(player)];

        String choice = getChoiceAsString("Do you pickup the " + getItemName(getRoomItem(currentRoom)) + "? (y/n)");

        if(choice.equals("y")){
            itemTaken = true;
            typeMessage(getItemName(getRoomItem(currentRoom)) + " picked up.", 10);
            addItem(player, getRoomItem(currentRoom)); // Add item to inventory
            setRoomItem(currentRoom, null);
        }

        return itemTaken;
    } // END roomitemTaken

    //#endregion





    //#region Get/Set methods

        // Each "block" of methods handles a record fields get and set methods (per line)

        // Player methods
        public static int getPlayerHealth(player p) {return p.health;}
        public static void setPlayerHealth(player p, int health) {p.health = health;}
    
        public static int getPlayerSanity(player p) {return p.sanity;}
        public static void setPlayerSanity(player p, int sanity) {p.sanity = sanity;}
    
        public static boolean isPlayerDead(player p) {return p.isDead;}
        public static void setPlayerDead(player p, boolean isDead) {p.isDead = isDead;}
    
        public static boolean isIncantationSpoken(player p) {return p.incantationSpoken;}
        public static void setIncantationSpoken(player p, boolean incantationSpoken) {p.incantationSpoken = incantationSpoken;}
    
        public static int getPlayerCurrentRoomIndex(player p) {return p.currentRoomIndex;}
        public static void setPlayerCurrentRoomIndex(player p, int currentRoomIndex) {p.currentRoomIndex = currentRoomIndex;}
    
        public static items[] getPlayerInventory(player p) {return p.inventory;}
        public static void setPlayerInventory(player p, items[] inventory) {p.inventory = inventory;}
    
        public static items[] getPlayerAllItems(player p) {return p.allItems;}
        public static void setPlayerAllItems(player p, items[] allItems) {p.allItems = allItems;}
    
        public static room[] getPlayerRoomList(player p) {return p.roomList;}
        public static void setPlayerRoomList(player p, room[] roomList) {p.roomList = roomList;}
    
        public static Stack<Integer> getPlayerPrevRooms(player p) {return p.prevRooms;}
    
        public static monster[] getPlayerMonsterList(player p) {return p.monsterList;}
        public static void setPlayerMonsterList(player p, monster[] monsterList) {p.monsterList = monsterList;}
    

        // Items methods
        public static int getItemDamage(items i) {return i.itemDamage;}
        public static void setItemDamage(items i, int itemDamage) {i.itemDamage = itemDamage;}
    
        public static String getItemName(items i) {return i.itemName;}
        public static void setItemName(items i, String itemName) {i.itemName = itemName;}
    
        public static boolean itemExistsInRoom(items i) {return i.existsInRoom;}
        public static void setItemExistsInRoom(items i, boolean existsInRoom) {i.existsInRoom = existsInRoom;}
    

        // Room methods
        public static int getRoomIndex(room r) {return r.roomIndex;}
        public static void setRoomIndex(room r, int roomIndex) {r.roomIndex = roomIndex;}
    
        public static int[] getRoomIndexArray(room r) {return r.roomIndexArray;}
        public static void setRoomIndexArray(room r, int[] roomIndexArray) {r.roomIndexArray = roomIndexArray;}
    
        public static String getPathDescription(room r) {return r.pathDescription;}
        public static void setPathDescription(room r, String pathDescription) {r.pathDescription = pathDescription;}
    
        public static String getRoomDescription(room r) {return r.roomDescription;}
        public static void setRoomDescription(room r, String roomDescription) {r.roomDescription = roomDescription;}
    
        public static void setItemTaken(room r, boolean itemTaken) {r.itemTaken = itemTaken;}
    
        public static boolean roomHasMonster(room r) {return r.hasMonster;}
        public static void setRoomHasMonster(room r, boolean hasMonster) {r.hasMonster = hasMonster;}
    
        public static items getRoomItem(room r) {return r.item;}
        public static void setRoomItem(room r, items item) {r.item = item;}
    
        public static monster getRoomMonster(room r) {return r.monster;}
        public static void setRoomMonster(room r, monster monster) {r.monster = monster;}
    

        // Monster methods
        public static int getMonsterHealth(monster m) {return m.health;}
        public static void setMonsterHealth(monster m, int health) {m.health = health;}
    
        public static String getMonsterName(monster m) {return m.name;}
        public static void setMonsterName(monster m, String name) {m.name = name;}
    
        public static int getMonsterSanityImpact(monster m) {return m.sanityImpact;}
        public static void setMonsterSanityImpact(monster m, int sanityImpact) {m.sanityImpact = sanityImpact;}
    
        public static int getMonsterDamage(monster m) {return m.damage;}
        public static void setMonsterDamage(monster m, int damage) {m.damage = damage;}
    
        public static int getMonsterAttackChance(monster m) {return m.attackChance;}
        public static void setMonsterAttackChance(monster m, int attackChance) {m.attackChance = attackChance;}
    
        public static int getMonsterDodgeChance(monster m) {return m.dodgeChance;}
        public static void setMonsterDodgeChance(monster m, int dodgeChance) {m.dodgeChance = dodgeChance;}
    
        public static boolean isMonsterDead(monster m) {return m.isDead;}
        public static void setMonsterDead(monster m, boolean isDead) {m.isDead = isDead;}

    //#endregion





    //#region misc methods

    // Outputs a message and gets an input as a string
    public static String getChoiceAsString(String message){
        Scanner scanner = new Scanner(System.in);
        String input;
        final int TEXT_SPEED = 50;

        typeMessage(message, TEXT_SPEED);

        input = scanner.nextLine();

        return input;
    } // END inputInt


    // Outputs a message and gets an input as an int
    public static int getChoiceAsInt(String text){
        Scanner scanner = new Scanner(System.in);
        
        String input = getChoiceAsString(text);
        while(!isInteger(input)){
            input = getChoiceAsString("Invalid input. Enter an integer.");
        }

        int integer = Integer.parseInt(input);

        return integer;
    } // END inputInt


    // Method to check for a real input for integers
    public static boolean isInteger(String input){
        
        // Check for null or empty input
        if(input == null || input.isEmpty()){
            return false;
        }
    
        // If the string starts with a 0 and is not solely 0
        if(input.length() > 1 && input.charAt(0) == '0'){
            return false;
        }
    
        // Check each character to ensure its a digit
        for(int i = 0; i < input.length(); i++){
            char c = input.charAt(i);
            if(c < '0' || c > '9'){
                return false;
            }
        }
    
        return true;
    } // END isPositiveInteger


    // Method to type a message character by character
    public static void typeMessage(String input, int speed) {
        
        // Loop through the text
        for(int i = 0; i < input.length(); i++){

            // Format to character
            System.out.printf("%c", input.charAt(i));
            busyWait(speed); // Wait
        }
        System.out.println(""); // Add a space
        return;
    } // END typeMessage


    // Method to pause/wait CPU for a set length of time
    public static void busyWait(long waitTime){
        
        // Get current time
        long startTime = System.currentTimeMillis();

        // Subtract the start time from the current time
        // Do so until greater than the time to wait
        while(System.currentTimeMillis() - startTime < waitTime){
        }

        return;
    }

    //#endregion





    //#region large story elements

    // Introduction text for the game
    public static void introduction(){
        final int TEXT_SPEED = 10;
        
        String message1 = "You are part of a small, close-knit group of friends, seeking solace in the Appalachian wilds. "
        + "Packed for a month-long expedition, you set out at the dawn of autumn, the perfect time to witness the leaves turning fiery hues.";
        
        String message2 = "You park your truck and begin walking north.";

        String message3 = "After venturing for two weeks, the idea of wandering off the trail becomes a common sentiment among the group. "
        + "This deviation, initially undertaken with idle curiosity, soon turns into an unintended foray into regions older than memory. "
        + "Trees of unnatural thickness loom overhead, their coiling branches forming grotesque, whispering arches beneath which you walk. "
        + "Everyone acknowledges the heavy feeling in the air, though no one speaks of it. You venture deeper into the wilderness, straying further from the path.";

        String message4 = "Two days have passed since you left the trail. Formations of rocks - perhaps carved in forgotten epochs by hands long since turned to dust - lie "
        + "scattered among the descending hills in a way that feels intentional.";

        String message5 = "The aeolian sounds passing through the trees ceased yesterday, leaving only the sound of your group's idle chatter "
        + "and the dry scrape of boots against stone.";

        String message6 = "The sun, already low, sinks behind the jagged peaks with unnatural speed, casting shadows that twist and distort the dying light. "
        + "It is in this half-light, when the pallid sky takes on a sickly hue, that you feel a calling to the stones. Curiosity urges you to investigate, and so you do. " 
        + "The ground softens beneath your feet as you wander from the group, and then it gives way entirely, sending you tumbling into an abyss.";

        String message7 = "You awake, vision blurry. Looking up you see how far you have fallen. "
        + "Any chance of climbing back is already rendered impossible. Viewing your surroundings you notice two paths in this cavern.";
        
        // Print out the messages
        typeMessage(message1, TEXT_SPEED);
        typeMessage(message2, TEXT_SPEED);
        typeMessage(message3, TEXT_SPEED);
        typeMessage(message4, TEXT_SPEED);
        typeMessage(message5, TEXT_SPEED);
        typeMessage(message6, TEXT_SPEED);
        typeMessage(message7, TEXT_SPEED);

        return;
    } // END introduction

    
    // Method for the altar room
    public static player altar(player player){
        final int TEXT_SPEED = 10;

        String message1 = "In the depths of this accursed cavern the atmosphere grows dense, almost choking you, and a green light emanates from some cubic monolith.";
        String message2 = "Stalactites hang around this centrepiece in an unnatural pattern, curving into it like they are being dragged into its mass.";
        String message3 = "This cube, wrought of stone darker than void, bears ancient symbols, cryptic and blasphemous.";
        String message4 = "A faint whisper parades across the room to your ears, spiralling your mind into unease.";
        String message5 = "Upon the cube lays a scroll. Its text uncomprehensible yet literate.";

        String message6 = "You speak the text and feel a wave of unknowing wash over your mind";
        String message7 = "You think it wise to not speak these words and leave.";

        String message8 = "You leave the room having already spoken the text";

        typeMessage(message1, TEXT_SPEED);
        typeMessage(message2, TEXT_SPEED);
        typeMessage(message3, TEXT_SPEED);
        typeMessage(message4, TEXT_SPEED);

        // If incantation is already spoken leave here
        if(isIncantationSpoken(player)){
            typeMessage(message8, TEXT_SPEED);
            return player;
        }

        typeMessage(message5, TEXT_SPEED);

        // Get choice for incantation
        String choice = getChoiceAsString("Do you speak the text? (y/n)");
        while(!choice.equals("y") || !choice.equals("n")){
            
            if(choice.equals("y")){
                setIncantationSpoken(player, true);
                typeMessage(message6, TEXT_SPEED);
                return player;
            }else if(choice.equals("n")){
                typeMessage(message7, TEXT_SPEED);
                return player;
            }else{
                System.out.println("Invalid input. Enter y/n.");
                choice = getChoiceAsString("Do you speak the text? (y/n)");
            }
        }

        return player;
    } // END altar


    // Method for the final room in the game
    public static void nyGothor(player player){
        final int TEXT_SPEED = 10;

        String message1 = "As you wander deeper into the cavern you find your spirit, your very soul, weighed down by the air - as if a dark blanket lay upon you. ";
        String message2 = "The passage seems to twist unnaturally in a pattern mimicking that of a spiral, as though reality itself grows pliable. ";
        String message3 = "The stones around you grow darker till they become uniform, only identifiable by the starry reflection cast upon them by your ever weakening light.";

        String message4 = "The passage begins to widen as a cacophonous sound shakes your mind.";
        String message5 = "Before you lies an abomination that words of this language cannot describe - a being whose very existence defies the fragile laws of the world you thought you knew. ";
        String message6 = " Its form writhes and shifts ceaselessly, an entropic mass of tendrils and limbs oscillating with no logical pattern. ";
        String message7 = "Your eyes, though terrified to bear witness, catch glimpses of numerous orbs glowing with an unnatural, malignant light. ";
        String message8 = "These eyes - if eyes they can be called - stare through you, as if they perceive more than your flesh, as if they perceive the very essence of your existence.";
        
        String message9 = "The air here hums as if it were in pain, bending to the will of the creature. ";
        String message10 = "The walls of this chamber could not be discerned from that of the night sky - calling them cosmic would hardly describe it - yet through your paralysed gaze you notice carving of ancient symbols, older than humanity itself, pulsing.";


        // With incantation
        String message11 = "You feel these marking twist your perception but you maintain your grasp on reality. Beyond the creature, at the farthest edge of the cavern, the abyss yawns wide.";
        String message12 = "It can not be defined as a chasm, rather a gaping void revealing the infinite darkness beyond. Your eyes get caught in this void, unable to deter the thought that something far greater lies within.";
        String message13 = "You feel a pull of madness there, a beckoning from the cosmos that promises knowledge - knowledge that will unravel your very soul.";
        String message14 = "You realise with sickening clarity that you are no longer a part of the world you once knew. You are but a fleeting speck before the vast forces that lurk beyond the stars…";
        String message15 = "Yet this does not deter you.";

        // Without incantation
        String message16 = "These markings twist your perception, filling your mind with fleeting glimpses of incomprehensible worlds beyond the veil of sanity. Voices whisper in your mind. You cannot understand what it is they speak of, yet their intent is clear.";
        String message17 = "Your body begins to move further into the chamber despite your attempts not to. The floor beneath you feels strangely soft, as if the stone itself is decaying. The voices grow louder.";
        String message18 = "You move to the centre of the room and notice you stand amongst a series of concentric rings. The creature looms over you as your vision begins to merge with the unknown.";
        String message19 = "You drop to your knees as the voices scream at you. Reaching into your back pocket you grasp onto your pocketknife and open it.";
        String message20 = "You penetrate your skin at the neck.";

        typeMessage(message1, TEXT_SPEED);
        typeMessage(message2, TEXT_SPEED);
        typeMessage(message3, TEXT_SPEED);
        typeMessage(message4, TEXT_SPEED);
        typeMessage(message5, TEXT_SPEED);
        typeMessage(message6, TEXT_SPEED);
        typeMessage(message7, TEXT_SPEED);
        typeMessage(message8, TEXT_SPEED);
        typeMessage(message9, TEXT_SPEED);
        typeMessage(message10, TEXT_SPEED);


        if(isIncantationSpoken(player)){
            // With incantation
            typeMessage(message11, TEXT_SPEED);
            typeMessage(message12, TEXT_SPEED);
            typeMessage(message13, TEXT_SPEED);
            typeMessage(message14, TEXT_SPEED);
            typeMessage(message15, TEXT_SPEED);
        }else{
            // No incantation
            typeMessage(message16, TEXT_SPEED);
            typeMessage(message17, TEXT_SPEED);
            typeMessage(message18, TEXT_SPEED);
            typeMessage(message19, TEXT_SPEED);
            typeMessage(message20, TEXT_SPEED);
        }

        return;
    } // END nyGothor


    // Method for player winning or "winning" the game
    public static void ending(player player){
        final int TEXT_SPEED = 10;

        String message1 = "The beast lies slain, a twisted ruin of blood and viscera strewn across the cavern floor, its unnatural form now a grotesque memory.";
        String message2 = "A chill wind sighs from the abyssal rift, carrying with it a disquieting resonance.";
        String message3 = "The corpse succumbs to the breeze, unravelling with unnatural haste into nothingness, its departure leaving behind an acrid tang that clings to the air.";

        // Good ending text
        String message4 = "In its absence, the shroud of darkness lifts, revealing a passage concealed in the stone - a crack holding a faint luminescence.";
        String message5 = "Driven by desperation to leave this place, you drag your battered body toward the opening. At its base, a flight of narrow steps spirals upward, their uneven contours carved with irregularity.";
        String message6 = "With no other recourse you brace yourself and begin the climb. Time bleeds into insignificance, and the journey becomes a blur of strained breath and trembling limbs.";
        String message7 = "It is as though the staircase itself conspires against you, extending its winding path far beyond comprehension.";
        String message8 = "At last, the oppressive dark yields to a blinding radiance. Sunlight strikes your face with an almost alien warmth, a piercing contrast to the cold of the depths below.";
        String message9 = "The wind, no longer heavy with subterranean whispers, now howls clean and sharp. Blinking against the brilliance, you emerge from a jagged fissure in a mountainside, hidden amidst a tangle of ancient stones.";
        String message10 = "Before you sprawls a valley cloaked in golden light, its contours familiar yet tinged with an uncanny, dreamlike haze. The sun hangs low.";
        String message11 = "Upon closer inspection you realise where you are. You know the way home.";

        // Bad ending text
        String message12 = "But the wind does not stop. It whispers, subtle yet unstoppable, threading into your mind with a vile intimacy. The murmurs slither like tendrils, pressing against the fragile walls of your sanity.";
        String message13 = "They speak no nameable language, yet their meaning saturates your being: surrender, descend, obey.";
        String message14 = "Your limbs betray you, moving as though guided by an unseen puppeteer. The whispers do not shout, for they have no need. You are but a vessel now, your will frail and broken.";
        String message15 = "The abyss yawns wide before you.";
        String message16 = "And then you fall.";
        String message17 = "Not with the terror of one cast into darkness, but with the terrible certainty of one fulfilling a long-ordained purpose. The air grows thick, cloying with the scent of decay.";
        String message18 = "The whispers swell to a symphony of triumph, their meaning now crystal-clear: there is no escape.";
        String message19 = "There never was.";


        typeMessage(message1, TEXT_SPEED);
        typeMessage(message2, TEXT_SPEED);
        typeMessage(message3, TEXT_SPEED);

        if(getPlayerSanity(player) > 90){
            // Bad ending
            typeMessage(message12, TEXT_SPEED);
            typeMessage(message13, TEXT_SPEED);
            typeMessage(message14, TEXT_SPEED);
            typeMessage(message15, TEXT_SPEED);
            typeMessage(message16, TEXT_SPEED);
            typeMessage(message17, TEXT_SPEED);
            typeMessage(message18, TEXT_SPEED);
            typeMessage(message19, TEXT_SPEED);
        }else{
            // Good ending
            typeMessage(message4, TEXT_SPEED);
            typeMessage(message5, TEXT_SPEED);
            typeMessage(message6, TEXT_SPEED);
            typeMessage(message7, TEXT_SPEED);
            typeMessage(message8, TEXT_SPEED);
            typeMessage(message9, TEXT_SPEED);
            typeMessage(message10, TEXT_SPEED);       
            typeMessage(message11, TEXT_SPEED);
        }

        return;
    } // END ending


    //#endregion

}


class player implements Serializable{
    int health;
    int sanity; // 0 is sane, 100 is bonkers!!!
    boolean isDead;
    boolean incantationSpoken;

    int currentRoomIndex;


    // Arrays for inventory, all items, rooms, and monsters
    items[] inventory;
    items[] allItems;
    room[] roomList;
    monster[] monsterList;

    Stack<Integer> prevRooms = new Stack<>();
}

class items implements Serializable{
    int itemDamage;
    String itemName;

    boolean existsInRoom; // Boolean to ensure there is only one of each item
}

class room implements Serializable{
    int roomIndex; // Used to identify room
    int[] roomIndexArray;
   
    String pathDescription;
    String roomDescription;

    boolean itemTaken;
    boolean hasMonster;

    // Item and monster in room
    items item;
    monster monster;
}

class monster implements Serializable{
    int health;
    
    String name;

    int sanityImpact; // How much sanity the monster drains from player
    int damage;
    int attackChance;
    int dodgeChance; // Chance to dodge player attack

    boolean isDead;
}
