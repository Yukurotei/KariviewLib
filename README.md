# KariView Library (PUBLIC ALPHA)
The ultimate minecraft HUD animation library, capable of displaying anything. Yes, of course you can make your powerpoint presentation with this.

This mod is for 1.20.1 FORGE

# Documentation (PLEASE READ CAREFULLY)

Below concludes all the information you need to make an animation.

For first timers, please read carefully. if you do something wrong the game WILL crash.

I advise you to learn how .json files works before continuing, as you will run into a lot of errors if you don't even know the basics.

### Directory structure

Everything is contained in your .minecraft/kariviewlib directory.

It is designed to be similar to how datapacks are organized.

For first timers: everything besides .minecraft/ and things fully capitalized are to be spelt out EXACTLY like how it is written, unless specified as examples or named as "example"

Word of advice: Don't copy and paste everything, type as the documentation goes.

```text
.minecraft/
    | kariviewlib
        | YOUR_NAMESPACE <- this is the namespace (your animation directory)
            | animations <- houses your animation files
                |> animation.json <------ these are examples
                |> anotherAnimation.json /
            | assets <- houses all your assets
                | sounds <- all your sound files (.ogg ONLY)
                    |> example.ogg
                    |> example2.ogg
                | textures <- all your images (png is recommended, does not support webp)
                    |> example.png
                    |> exampleSprite1.png
                    |> exampleSprite2.png
```

### Animation.json file structure

All animations and actions are inside a json file (.json, .json5 is fine as well)

```json5
{
  "id": "ANIMATION_IDENTIFIER", //This is the identifier for the animation. This isn't used and can be named anything.
  "elements": [ //This is where all the elements is initialized
    {

    }
  ],
  "keyframes": [ //Contains all the actions to be executed at a certain timestamp
    //NOTE: Timestamp is calculated at milliseconds
    {
      "timestamp": 0,
      "actions": [ //Contains all the actions to be executed
        {

        }
      ]
    },
    {
      "timestamp": 1000, //executes after one second (1000ms = 1s)
      "actions": [
        {
          
        }
      ]
    }
  ]
}
```

## Elements
Elements are defined in the elements field

An example would be:
```json5
"elements": [
    {
      "element_id": "example_image", //This id is what you put in for element_id fields
      "texture_path": "example.png" //This is the texture path of the image.
       //texture_path will automatically look in (look below) 
       //YOUR_NAMESPACE(name of your namespace)/assets/textures
       //For this example, you have to put example.png under assets/textures/
    },
    //Below is advanced usage of elements, takes advantage of the sprite animation system
    {
      "element_id": "animated_sprite",
      "texture_path_pattern": "megaknight*.png"
       //This is used when you have a sprite animation frames (e.g. megaknight1.png, megaknight2.png).
       //In this case, use * to replace where you have numbers. KEEP IN MIND this ALWAYS starts at 1, not 0 (will be ignored).
    }
],
```

Sounds are **not** defined here.

## Actions
### Note: All actions are to be written like this (in the actual .json file)
```json5
{
  "keyframes": [
    {
      "timestamp": 0, //This is just example timestamp, similar to above
      "actions": [
        {
          "type": "example_action", //EXAMPLE, usually will be the action you want to execute, like show_element
          "parameter1": 1, //Below are all examples, it will be different in real use
          "parameter2": true,
          "easing": "easeOutBack"
        },
        {
          "type": "example_action2",
          "parameter3": "blablabla"
        }
      ]
    },
    {
      "timestamp": 1000, //Example, executes after 1 second
      "actions": [
        {
          //...
        }
      ]
      //...
    }
  ]
}
```

Definitions of elements would be structured like this below:

(don't write this in the actual json file, this is just to tell you how the definitions will be listed below, and the parameters)

### ElementAction (this is not a real action)
```text
type: do_element <--This is what you actually WRITE in type, if you look up, this is what replaces example_action
(description will be located here)
argument1(type): what argument1 does
argument2(type): ....
```

For y'all non coders, here's all the types:

string - text

int - integers (has limit, check google)

long - integers but with more limit (still has limit)

float - decimal or integer (has limit, check google)

double - float but more limit (still has limits)

boolean - true or false (to be spelt out exactly how it's worded, all lower case)

## Below indicates all implemented actions (Sorted alphabetically)

Note: All X, Y, and scale related arguments are in PERCENTAGES(decimal form, e.g. 40% will be 0.4).

This is the case because every one plays the game on different resolution, this keeps it scaled and position correctly for everyone.

If the definition has SPRITES ONLY listed, it is only used for SPRITE ELEMENTS (the elements that use texture_path_pattern, or in other words the elements that uses the sprite animation system)

All durations and intervals CAN NOT BE ZERO

### ExtendElement
Warps the dimensions of the element
```text
type: extend_element
element_id(string): the id of the element you want to extend
target_value(double): the target scale to extend to
duration(long): how long this action will last (speed in some way)
direction(string): could be "UP", "DOWN", "LEFT" or "RIGHT"
easing_type(string): type of easing, relate to easings section below
```

### HideElementAction
Hides an element (additionally, resets the dimensions of the element.)
```text
type: hide_element
element_id(string): the id of the element you want to hide
```

### MoveAction
Moves an element
```text
type: move_element
element_id(string): the id of the element you want to move
target_x(double): the x coordinate to move to
target_y(double): the y coordinate to move to
duration(long): how long the move will last (speed in some way)
easing_type(string): type of easing, relate to easings section below
```

### PlaySoundAction
Plays an audio file in asset/sounds
```text
type: play_sound
sound_id(string): the file to play. This will automatically look in YOUR_NAMESPACE/assets/sounds.
volume(float): the volume to play the file at (in PERCENTAGES decimal form, e.g. 1 will be 100% volume)
```

### RegisterAudioElementAction
Makes the target element react to audio
```text
type: register_audio_element
element_id(string): the element to register as an audio element
sensitivity(float): the sensitivity which it detects band energy (if you don't know what it means just play with it)
max_hertz(int): the maximum hertz of the audio it reacts to
min_hertz(int): the minimum hertz of the audio it reacts to
max_volume(float): the volume at which the effects will reach its highest value
effects(List<AudioEffects>): what it will do when a beat is detected. Relate to effects section below
easing_type(string): type of easing, relate to easings section below
```

### RotateAction
Rotates the element
```text
type: rotate_element
element_id(string): the element to rotate
target_angle(double): the degree of the angle (can go over 360)
duration(long): how long the rotation should last (speed in some way)
easing_type(string): type of easing, relate to easings section below
```

### ScaleAction
Scales the element
```text
type: scale_element
element_id(string): the element to scale
target_scale(double): the target scale to scale to
duration(long): how long the scaling should last
easing_type(string): type of easing, relate to easings section below
```

### SetSpriteIndex (SPRITES ONLY)
Sets the index of the sprite animation
```text
type: set_sprite_index
element_id(string): the sprite element to set index of
target_index(int): the index to set to (will be ignored if the index is out of bounds and loop is false)
loop(boolean): if the index should loop (e.g. if you set index to 5 but you only have 4 frames, it will loop to 1)
```

### ShowElementAction
Shows the element (ANYTHING THAT IS EXECUTED BEFORE THIS ACTION WILL BE IGNORED/RESULT IN ERROR)
```text
type: show_element
element_id(string): the element to show
x(double): where to show the element horizontally (x percentage of the screen)
y(double): where to show the element vertically (y percentage of the screen)
scale(double): the scale to show it as
texture_width(int): the width of the texture (you should check your image's properties for this value)
texture_height(int): the height of the texture (you should check your image's properties for this value)
```

### StepSpriteIndexAction (SPRITES ONLY)
Steps a certain amount of indexes forward
```text
type: step_sprite_index
element_id(string): the sprite to step index of
steps(int): amount of indexes to step forward
loop(boolean): should loop or just stay at max (e.g. if it is at maximum index but still has steps, it'll go back to 1)
duration(long): the duration of the stepping
```

### StopAllSoundsAction
Stops all sounds
```text
type: stop_all_sounds
```

### StopSpriteAnimationAction (SPRITE ONLY)
Stops the automatic updating of a sprite (stops it from being updated by UpdateSpriteAction)
```text
type: stop_sprite_animation
element_id(string): the sprite to stop updating
```

### UnregisterAudioElementAction
Unregisters an element as audio element (DOES NOT RESET ITS SCALE/DIMENSIONS)
```text
type: unregister_audio_element
element_id(string): the element to unregister
```

### UpdateSpriteAction (SPRITE ONLY)
Updates a sprite based on a certain interval (steps it forward one index every time it updates)
```text
type: update_sprite
element_id(string): the sprite to update
update_interval(long): the duration (in milliseconds) that it waits before updating
loop(boolean): should it loop or not (e.g. when it reaches the maximum index, it will go back to one if it is true)
```

# Easings
Easings basically smooths the progress of animation. The easings are implemented based on easings.net

You can go to easings.net to view the effects of the easing

Here's all the easings that are implemented (THIS IS TO BE SPELLED OUT ACCURATELY, EVEN CASES)
```text
linear – constant speed, no acceleration.

easeInQuad – starts slow, accelerates quickly.

easeOutQuad – starts fast, slows down at the end.

easeInOutQuad – slow at start and end, faster in the middle.

easeInCubic – very slow start, speeds up strongly.

easeOutCubic – fast start, slows down smoothly.

easeInOutCubic – slow → fast → slow, smoother than Quad.

easeInQuart – extremely slow start, strong acceleration.

easeOutQuart – very fast start, eases out strongly.

easeInOutQuart – dramatic slow → fast → slow curve.

easeInQuint – almost flat start, very sharp acceleration.

easeOutQuint – very sharp start, smooth finish.

easeInOutQuint – extreme slow → explosive middle → gentle end.

easeInSine – starts gently, speeds up like a sine wave.

easeOutSine – starts fast, eases out gently like a sine wave.

easeInOutSine – soft, wave-like acceleration and deceleration.

easeInExpo – almost no movement at first, then rockets forward.

easeOutExpo – rockets forward, then eases to a stop.

easeInOutExpo – slow start → explosive middle → gentle stop.

easeInCirc – starts very slow (circular curve), accelerates.

easeOutCirc – starts fast, slows dramatically near the end.

easeInOutCirc – circular motion feel, slow edges, fast middle.

easeInBack – pulls slightly backward, then shoots forward.

easeOutBack – overshoots the end, then settles back.

easeInOutBack – pulls back, speeds through, overshoots, then settles.
```

# Effects
Exclusively used by RegisterAudioElementAction

Effects are used like this (this is an example of register_audio_element):

```json5
{
  "type": "register_audio_element",
  "element_id": "example",
  "sensitivity": 0.8,
  "max_hertz": 120,
  "min_hertz": 10,
  "max_volume": 2,
  "effects": [ //Pay attention to here
    {
      "type": "STEP_SPRITE",
      "step": 1,
      "loop": true,
      "delay": 150
    },
    {
      "type": "EXTEND",
      "target_value": 2,
      "direction": "LEFT",
      "default_value": 1,
      "decay": 150,
      "extend_duration": 100
    }
  ],
  "easing_type": "easeInOutBack"
}
```
Current effects include:
### ExtendEffect
Warps the dimension of the element
```text
type: EXTEND,
direction(string): the direction it shall warp, can be "UP", "DOWN", "LEFT" or "RIGHT"
target_value(double): the maximum scale it shall warp to
decay(double): the duration of the shrinkage of the element if it doesn't detect a beat
default_value(double): the default value (minimum) of the scale of the element
extend_duration(double): how long the extension will last if it detects a beat
```

### PulseEffect
Pulses the element
```text
type: PULSE
decay(double): the duration of the shrinkage of the element if it doesn't detect a beat
default_scale(float): the default scale (minimum) of the scale of the element
max_scale(float): the maximum scale it should be pulsed  
```

### StepSpriteEffect (SPRITE ONLY)
Steps the sprite step amount of times when it detects a beat (this one has no animation)
```text
type: STEP_SPRITE
step(int): the amount of step it takes when it detects a beat
loop(boolean): should it loop or not
duration(int): how long before the sprite can be stepped again (so two StepSpriteEffects doesn't occur at the same time)
```

# Commands
To play an animation:
```text
/kariview playAnimation
```

I think you can figure the rest out by yourself :D

# Developer stuff

## Contribution
Currently, won't take any requests until I think I am ready