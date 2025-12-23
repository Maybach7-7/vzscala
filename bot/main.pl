:- use_module(library(http/websocket)).
:- use_module(library(http/json)).
:- use_module(library(http/json_convert)).

% Initialize game state
init_game :-
    retractall(room_id(_)),
    retractall(player_id(_)),
    retractall(opponent_connected(_)),
    retractall(game_ready(_)),
    assert(opponent_connected(false)),
    assert(game_ready(false)).

% Random move selection
random_move(Move) :-
    random_member(Move, ['rock', 'paper', 'scissors']).

% Handle server messages
handle_message(Msg) :-
    ( catch(
        setup_call_cleanup(
            open_string(Msg, Stream),
            json_read_dict(Stream, Dict),
            close(Stream)
        ),
        Error,
        (writeln('Failed to parse JSON'), writeln(Msg), writeln(Error), fail)
    ) ->
        handle_parsed_message(Dict)
    ;
        writeln('JSON parsing failed completely')
    ).

handle_parsed_message(Dict) :-
    ( get_dict(type, Dict, Type) ->
        (
            Type == "ready" ->
                handle_ready(Dict)
            ;
            Type == "result" ->
                handle_result(Dict)
            ;
            Type == "error" ->
                handle_error(Dict)
            ;
                format('Unknown message type: ~w~n', [Type])
        )
    ;
        format('Message missing type field: ~w~n', [Dict])
    ).

handle_ready(_Dict) :-
    writeln('Game is ready!'),
    assert(game_ready(true)),
    format('Sending moves to room...~n').

handle_result(Dict) :-
    get_dict(winner, Dict, Winner),
    get_dict(player1Score, Dict, Score1),
    get_dict(player2Score, Dict, Score2),
    format('Round result - Winner: ~w, Scores: ~w-~w~n', [Winner, Score1, Score2]).

handle_error(Dict) :-
    get_dict(message, Dict, Message),
    format('Error received: ~w~n', [Message]).

% Send move to server
send_move(WS) :-
    game_ready(true),
    !,
    random_move(Move),
    room_id(RoomId),
    player_id(PlayerId),
    format(atom(Json), '{"type":"move","roomId":"~w","playerId":"~w","choice":"~w"}', [RoomId, PlayerId, Move]),
    format('Sending move: ~w~n', [Json]),
    ws_send(WS, text(Json)).
send_move(_) :-
    writeln('Game not ready, not sending move').

% Main game loop
game_loop(WS) :-
    catch(
        ws_receive(WS, Msg),
        _,
        (writeln('Error receiving message'), halt)
    ),
    (
        Msg.opcode == close ->
            writeln('Connection closed by server'),
            halt
    ;
        % Process incoming message
        Msg.data = Data,
        catch(
            handle_message(Data),
            _,
            writeln('Error handling message')
        ),

        % Possibly send a move if game is ready
        catch(
            send_move(WS),
            _,
            writeln('Error sending move')
        ),

        % Continue the loop
        game_loop(WS)
    ).

% Connect to server
connect_to_server(URL, Room, Player) :-
    atom_string(URLStr, URL),
    catch(
        http_open_websocket(URLStr, WS, []),
        _,
        (writeln('Failed to connect to server'), halt)
    ),
    writeln('Connected to server'),

    % Store identifiers
    retractall(room_id(_)), assert(room_id(Room)),
    retractall(player_id(_)), assert(player_id(Player)),

    % Send join message
    format(atom(JoinMsg), '{"type":"join","roomId":"~w","playerId":"~w"}', [Room, Player]),
    ws_send(WS, text(JoinMsg)),
    writeln('Sent join message'),

    % Start game loop
    game_loop(WS).

% Main entry point
start_bot :-
    getenv('WS_URL', WS_URL),
    (getenv('ROOM_ID', ROOM_ID) -> true ; ROOM_ID = 'default-room'),
    random_between(1000, 9999, RandomId),
    atomic_list_concat(['bot-', RandomId], '', BotId),

    format('Bot starting...~n'),
    format('WebSocket URL: ~w~n', [WS_URL]),
    format('Room ID: ~w~n', [ROOM_ID]),
    format('Player ID: ~w~n', [BotId]),

    init_game,
    connect_to_server(WS_URL, ROOM_ID, BotId).

:- initialization(start_bot, main).
