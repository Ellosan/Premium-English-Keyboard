package com.premiumenglish.keyboard

/**
 * Lexicon — the vocabulary of Premium English.
 *
 * Tables are keyed by service tier:
 *   1 = Refined    light elevation, plain pronouns
 *   2 = Courtly    thou/thee, -est/-eth, the archaic wardrobe
 *   3 = Sovereign  the full ceremonial treatment
 *
 * A translation at tier N applies every table from tier 1 up to tier N, so the
 * higher tiers are written as refinements of the lower ones ("house" becomes
 * "residence" at tier 1, and "residence" becomes "abode" at tier 2).
 *
 * The tables are stored as text and parsed at class-load. It keeps them
 * readable and keeps the generated static initialiser well clear of the JVM's
 * 64 KB method limit, which large map literals run into surprisingly quickly.
 */
object Lexicon {

    // ---------------------------------------------------------------- contractions

    /** Expanded before anything else touches the text. */
    val CONTRACTIONS: Map<String, String> = LexiconText.pairs(
        """
        i'm = i am
        i've = i have
        i'll = i will
        i'd = i would
        you're = you are
        you've = you have
        you'll = you will
        you'd = you would
        he's = he is
        she's = she is
        it's = it is
        that's = that is
        there's = there is
        here's = here is
        what's = what is
        who's = who is
        where's = where is
        when's = when is
        how's = how is
        let's = let us
        we're = we are
        we've = we have
        we'll = we will
        we'd = we would
        they're = they are
        they've = they have
        they'll = they will
        they'd = they would
        can't = can not
        cannot = can not
        won't = will not
        don't = do not
        doesn't = does not
        didn't = did not
        isn't = is not
        aren't = are not
        wasn't = was not
        weren't = were not
        haven't = have not
        hasn't = has not
        hadn't = had not
        wouldn't = would not
        shouldn't = should not
        couldn't = could not
        mustn't = must not
        gonna = going to
        wanna = want to
        gotta = got to
        kinda = kind of
        sorta = sort of
        lemme = let me
        gimme = give me
        y'all = you all
        ain't = is not
        """
    )

    // ---------------------------------------------------------------- phrases

    /** Multi-word phrases, written as they appear after contractions expand. */
    private val PHRASES_1 = LexiconText.pairs(
        """
        thank you very much = I thank you most heartily
        thanks a lot = I thank you most heartily
        thank you = I thank you
        excuse me = I beg your pardon
        a lot of = a great many
        lots of = a great many
        a bunch of = a host of
        a lot = a great deal
        what kind of = what manner of
        kind of = somewhat
        sort of = somewhat
        right now = this very instant
        of course = but naturally
        no problem = it is nothing
        have to = must
        has to = must
        got to = must
        need to = must
        want to = wish to
        figure out = determine
        find out = discover
        get home = return home
        see you later = until we meet again
        see you soon = until we meet again
        talk to you later = we shall speak again
        what is up = how goes it
        make sure = see to it
        hurry up = make haste
        come on = come now
        hang out = keep company
        take care = keep well
        good luck = may fortune favour you
        happy birthday = many happy returns
        in a bit = shortly
        for real = in earnest
        big deal = matter of great weight
        so what = what of it
        my bad = the fault is mine
        let me know = send me word
        text me = send me word
        call me = summon me
        check out = examine
        look at = observe
        shut up = hold your tongue
        hold on = one moment
        i guess = I suppose
        oh my god = good heavens
        what the hell = what the devil
        no way = out of the question
        """
    )



    // ---------------------------------------------------------------- single words

    private val WORDS_1 = LexiconText.pairs(
        """
        hello = greetings
        hi = greetings
        hey = I say
        bye = farewell
        goodbye = farewell
        yeah = indeed
        yep = indeed
        yes = indeed
        nope = no
        ok = very well
        okay = very well
        sure = assuredly
        please = kindly
        sorry = my apologies
        very = most
        really = truly
        actually = in truth
        literally = truly
        probably = in all likelihood
        definitely = assuredly
        maybe = perhaps
        totally = entirely
        basically = in essence
        obviously = plainly
        also = likewise
        however = nevertheless
        anyway = in any case
        about = concerning
        while = whilst
        among = amongst
        toward = towards
        big = considerable
        huge = immense
        small = modest
        little = slight
        good = fine
        great = splendid
        bad = unfortunate
        awful = deplorable
        awesome = magnificent
        amazing = remarkable
        cool = admirable
        nice = agreeable
        beautiful = exquisite
        pretty = handsome
        weird = peculiar
        strange = curious
        funny = droll
        boring = tedious
        hard = arduous
        tough = formidable
        smart = learned
        dumb = unlettered
        stupid = ill-considered
        rich = wealthy
        tired = weary
        happy = delighted
        sad = downcast
        angry = displeased
        scared = apprehensive
        afraid = apprehensive
        hungry = famished
        thirsty = parched
        sick = indisposed
        crazy = unhinged
        busy = much occupied
        get = obtain
        buy = procure
        sell = vend
        give = bestow
        send = dispatch
        make = craft
        build = construct
        fix = mend
        help = assist
        try = endeavour
        use = employ
        show = reveal
        bring = fetch
        start = commence
        begin = commence
        finish = conclude
        stop = cease
        talk = converse
        ask = inquire
        want = desire
        need = require
        love = adore
        hate = detest
        see = perceive
        watch = observe
        listen = attend
        leave = depart
        stay = remain
        live = reside
        thing = matter
        things = matters
        stuff = sundries
        problem = difficulty
        idea = notion
        plan = design
        news = tidings
        story = account
        job = position
        work = labour
        money = funds
        house = residence
        home = household
        food = refreshment
        friend = companion
        friends = companions
        guy = gentleman
        people = persons
        kid = child
        kids = children
        car = motor car
        phone = telephone
        party = gathering
        fun = diversion
        game = contest
        soon = shortly
        later = in due course
        always = ever
        """
    )




    /**
     * The rest of the Courtly vocabulary.
     *
     * Kept in its own table only for length. Words whose sense changes with
     * their grammar are deliberately absent — "will", "may", "might", "can",
     * "just", "like", "so", "well", "mean", "back" and "right" all mean two
     * things at once, and replacing them turns sentences to nonsense. The stem
     * lookup in the engine means one entry here also covers the plural, the
     * past tense and the -ing form.
     */

    /**
     * Tier one ships with the app. The Courtly and Sovereign tables belong to
     * the Pro edition and are supplied by [Edition]; in the free edition they
     * are simply not there, which is what makes the free build free of them.
     */
    val PHRASES: Map<Int, Map<String, String>> = mapOf(1 to PHRASES_1) + Edition.phrases
    val WORDS: Map<Int, Map<String, String>> = mapOf(1 to WORDS_1) + Edition.words

    // ---------------------------------------------------------------- grammar data

    /** Base verbs we are confident enough about to conjugate. */
    val VERBS: Set<String> = LexiconText.words(
        """
        abide accept ache act add admire admit advise agree aim allow answer
        appear apply argue arrive ask attack attend avoid awake bake bear beat
        beg begin behold believe belong bend beseech bestow bide bind bite bless
        blow boast break breathe bring build burn buy call care carry catch
        change charge chase cheer cherish choose claim clean climb close come
        command complain conclude consider continue cook count cover covet crave
        create cross cry cut dance dare deal decide declare deem defend deliver
        demand deny depart describe deserve desire destroy die dig discover
        discuss dive divine do doubt drag draw dream dress drink drive drop dwell
        earn eat employ end endure enjoy enter escape espy expect explain fail
        fall fancy fare feed feel fetch fight fill find finish fit fix flee fly
        follow forget forgive forfeit form free gain gather get give go grant
        greet grow guard guess handle hang happen hark harken hate haunt heal
        hear hearken help hide hire hold hope hunt hurry hurt imagine intend
        invite join judge jump keep kill kiss knock know lack land last laugh lay
        lead leap learn leave lend let lie lift like listen live loathe lock long
        look lose love make march mark marry mean meet mend mind miss move name
        need note notice obey offer open order own pass pay peruse pick place
        plan play please point possess pour pray prefer prepare present press
        prevail proceed promise prove provide pull push put quaff question raise
        reach read realise realize receive reckon recall refuse regard remain
        remember remove rend repair reply report require rest return ride ring
        rise roam rule run rush sail save say search see seek seem sell send
        serve set settle shake share shine shoot shout show shut sigh sing sit
        slay sleep slumber smell smile solve sound speak spend stand stare start
        stay steal step stop strike strive study succeed suffer suggest sup
        suppose swear swim take talk tarry teach tell tend thank think throw
        touch train travel treat trust try turn understand use vanish vex visit
        vow wait wake walk wander want warn wash watch wear weep weigh welcome
        win wish wonder work worry write yield
        achieve affect base become cause check compare complete connect contain
        control cost determine develop encourage ensure establish examine exist
        express face focus force generate hit identify ignore improve include
        increase indicate influence inform introduce involve limit link list
        maintain manage matter measure mention occur operate organise organize
        perform permit prevent print produce protect publish purchase reduce
        refer reflect reject relate release rely repeat replace represent result
        reveal review risk roll satisfy select separate shape sign sink sort
        split spread state stick supply support survive test tie vote waste wave
        cheat rob chase attack beat destroy cut push pull lift drop hit jump
        travel rest wake cry call shout dance argue agree allow promise forgive
        praise complain decide prepare understand change create happen marry hug
        heal rescue protect wash clean dress worry stress trouble guard group
        team plan map box picture video photo camera coat dress gown drop
        """
    )

    /** Irregular second-person-singular forms, used after "thou". */
    val IRREGULAR_2SG: Map<String, String> = LexiconText.pairs(
        """
        am = art
        are = art
        is = art
        was = wast
        were = wert
        be = beest
        have = hast
        has = hast
        had = hadst
        do = dost
        does = dost
        did = didst
        will = wilt
        would = wouldst
        shall = shalt
        should = shouldst
        can = canst
        could = couldst
        may = mayst
        might = mightst
        must = must
        ought = oughtest
        say = sayest
        go = goest
        know = knowest
        see = seest
        make = makest
        take = takest
        come = comest
        give = givest
        think = thinkest
        speak = speakest
        hear = hearest
        let = lettest
        """
    )

    /** Irregular third-person-singular forms. */
    val IRREGULAR_3SG: Map<String, String> = LexiconText.pairs(
        """
        has = hath
        does = doth
        says = saith
        goes = goeth
        is = is
        was = was
        did = did
        had = had
        """
    )

    /** Words that may sit between "thou" and the verb it governs. */
    val INTERPOSED_ADVERBS: Set<String> = LexiconText.words(
        """
        not never ne'er ever e'er always oft often truly verily really surely
        sore most still yet but merely only also likewise then now presently
        anon thus therefore indeed exceeding
        """
    )

    /** Subjects that put the -eth ending on the verb that follows. */
    val THIRD_SINGULAR_SUBJECTS: Set<String> = LexiconText.words(
        """
        he she it who which one everyone someone anyone nobody everybody
        somebody anybody god fate fortune heaven lord lady king queen knight
        fellow soul man woman hound steed world sun moon night day wind sea
        heart mind life love death time truth beauty this that everything
        something nothing anything
        """
    )

    /** Subjects that never take -eth or -est. */
    val PLURAL_SUBJECTS: Set<String> = LexiconText.words("i we they ye you these those folk people")

    /** After one of these, a word ending in -s is almost certainly a plural noun. */
    val DETERMINERS: Set<String> = LexiconText.words(
        """
        the a an my thy thine your his her its our their these those some many
        few several all both each every any no two three four five ten other
        another such more most less least much of in on at for with from by
        about into over under through between betwixt among amongst
        """
    )

    /** Words ending in -s that are never third-person verbs. */
    val NEVER_ETH: Set<String> = LexiconText.words(
        """
        is was has does as his hers its us this thus always perhaps sometimes
        news yes plus less unless
        """
    )

    /**
     * Words ending in -s that are usually plural nouns. These are only held
     * back when the subject in front of them is not clearly singular, so
     * "he plays" still becomes "he playeth" while "good times" is left alone.
     */
    val NOUN_LIKE_S: Set<String> = LexiconText.words(
        """
        things matters times ways means eyes hands days years words results
        answers questions places faces kids lives games plans forms lights
        minds notes marks points orders reports returns shares sounds states
        steps trains travels visits walks watches wishes wonders yields changes
        shows plays works uses offers presents rests stops starts tastes trusts
        signs links lists tests votes waves shapes prints risks reviews costs
        limits measures controls results states forms faces
        """
    )

    /** Irregular past tenses, which the base-verb list cannot reach. */
    val PAST_VERBS: Set<String> = LexiconText.words(
        """
        saw told gave took made said knew thought heard found brought sent met
        left felt kept held put set came went got had was were did began broke
        brought bought caught chose drank drove ate fell fought forgot grew
        knew laid lay led lost paid ran rang rose sat slept spoke spent stood
        stole taught tore thought threw understood woke wore won wrote
        """
    )

    /** Irregular past tenses, mapped back to the verb they came from. */
    val PAST_TO_BASE: Map<String, String> = LexiconText.pairs(
        """
        bought = buy
        brought = bring
        caught = catch
        chose = choose
        came = come
        did = do
        drove = drive
        ate = eat
        fell = fall
        felt = feel
        fought = fight
        found = find
        flew = fly
        forgot = forget
        gave = give
        got = get
        grew = grow
        heard = hear
        held = hold
        hid = hide
        kept = keep
        knew = know
        led = lead
        left = leave
        lost = lose
        made = make
        met = meet
        paid = pay
        ran = run
        rode = ride
        rose = rise
        said = say
        saw = see
        sat = sit
        sent = send
        shot = shoot
        slept = sleep
        sold = sell
        spent = spend
        spoke = speak
        stole = steal
        stood = stand
        swam = swim
        taught = teach
        thought = think
        threw = throw
        told = tell
        took = take
        understood = understand
        went = go
        wept = weep
        wore = wear
        won = win
        wrote = write
        broke = break
        began = begin
        drank = drink
        sang = sing
        """
    )

    /** The past tense of the archaic verbs the lexicon hands out. */
    val IRREGULAR_PAST: Map<String, String> = LexiconText.pairs(
        """
        weep = wept
        bear = bore
        draw = drew
        strike = struck
        cleave = cleft
        flee = fled
        seek = sought
        behold = beheld
        rend = rent
        slay = slew
        dwell = dwelt
        wed = wed
        leap = leapt
        creep = crept
        sleep = slept
        keep = kept
        hold = held
        tell = told
        take = took
        make = made
        come = came
        go = went
        give = gave
        get = got
        stand = stood
        teach = taught
        buy = bought
        bring = brought
        think = thought
        speak = spoke
        write = wrote
        hurl = hurled
        """
    )

    /** Prepositions after which "you" is an object, and so becomes "thee". */
    val PREPOSITIONS: Set<String> = LexiconText.words(
        """
        to for with at of from on in by about like unto upon near before after
        than between betwixt among amongst without within toward towards
        against beside behind beneath beyond ere
        """
    )

    /** Auxiliaries after which "you" is still the subject, and so stays "thou". */
    val AUXILIARIES: Set<String> = LexiconText.words(
        """
        do does did are were is was will would shall should can could may might
        must have has had dost doth art wilt shalt canst
        """
    )

    // ---------------------------------------------------------------- ceremony

    /** Sentence openers, added only when a sentence is finished. */
    val OPENERS: Map<Int, List<String>> =
        mapOf(1 to listOf("Indeed,", "Truly,", "I must say,")) + Edition.openers

    /** Sentence closers, added only when a sentence is finished. */
    val CLOSERS: Map<Int, List<String>> =
        mapOf(1 to listOf(", to be sure", ", I should think")) + Edition.closers

    /** Faux-antique respellings, a Sovereign feature and so a Pro one. */
    val OLDE_SPELLINGS: Map<String, String> = Edition.oldeSpellings
}
