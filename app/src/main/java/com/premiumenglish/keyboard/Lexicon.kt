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

    private fun pairs(spec: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (raw in spec.trimIndent().lineSequence()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val i = line.indexOf('=')
            if (i <= 0) continue
            out[line.substring(0, i).trim()] = line.substring(i + 1).trim()
        }
        return out
    }

    private fun words(spec: String): Set<String> =
        spec.trim().split(Regex("[\\s,]+")).filter { it.isNotEmpty() }.toHashSet()

    // ---------------------------------------------------------------- contractions

    /** Expanded before anything else touches the text. */
    val CONTRACTIONS: Map<String, String> = pairs(
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
    private val PHRASES_1 = pairs(
        """
        thank you very much = I thank you most heartily
        thanks a lot = I thank you most heartily
        thank you = I thank you
        excuse me = I beg your pardon
        a lot of = a great many
        lots of = a great many
        a bunch of = a host of
        a lot = a great deal
        kind of = somewhat
        sort of = somewhat
        right now = this very instant
        of course = but naturally
        no problem = it is nothing
        i think = methinks
        i thought = methought
        it seems = meseems
        have to = must needs
        has to = must needs
        got to = must needs
        need to = must needs
        want to = wish to
        figure out = divine
        find out = discover
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
        happy birthday = joyous natal day
        in a bit = shortly
        for real = in earnest
        big deal = matter of great weight
        so what = what of it
        my bad = the fault is mine
        let me know = send me word
        text me = send me word
        call me = summon me
        check out = behold
        look at = behold
        shut up = hold your tongue
        hold on = hold, I pray
        i guess = I suppose
        oh my god = by my troth
        what the hell = what the devil
        no way = nay, never
        """
    )

    private val PHRASES_2 = pairs(
        """
        good morning = good morrow
        good afternoon = good day to you
        good evening = good even
        good night = good even
        how are you = how dost thou fare
        how is it going = how goes it
        what is up = how fares it with thee
        see you later = until we meet anon
        see you soon = until we meet anon
        talk to you later = we shall speak anon
        i love you = I do adore thee
        i miss you = I pine for thee
        are you kidding me = dost thou jest
        just kidding = I do but jest
        you all = ye
        you guys = ye gentles
        you two = ye both
        thank you very much = I thank thee most heartily
        thank you = I thank thee
        excuse me = I pray thee, pardon
        i am sorry = I crave thy pardon
        shut up = hold thy tongue
        hold your tongue = hold thy tongue
        take care = fare thee well
        good luck = fortune favour thee
        let me know = send me word by swift rider
        no problem = 'tis nothing
        """
    )

    private val PHRASES_3 = pairs(
        """
        good morning = a most gracious morrow unto thee
        i do not know = I know not
        i do not care = I care not
        what is up = what tidings, gentle soul
        see you later = until the wheel of fortune turns us together again
        thank you = my everlasting thanks unto thee
        no problem = think nothing on it
        right now = upon this very instant, and not a moment beyond
        """
    )

    // ---------------------------------------------------------------- single words

    private val WORDS_1 = pairs(
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
        please = if you please
        sorry = my apologies
        very = most
        really = truly
        actually = in truth
        literally = verily
        probably = in all likelihood
        definitely = assuredly
        maybe = perhaps
        totally = entirely
        basically = in essence
        obviously = plainly
        also = likewise
        however = howbeit
        anyway = howsoever
        about = concerning
        while = whilst
        between = betwixt
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
        wait = tarry
        talk = converse
        ask = inquire
        want = desire
        need = require
        love = adore
        hate = detest
        think = consider
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
        guys = company
        people = persons
        kid = child
        kids = children
        car = motor carriage
        phone = telephone
        party = gathering
        fun = diversion
        game = contest
        soon = shortly
        later = in due course
        always = ever
        """
    )

    private val WORDS_2 = pairs(
        """
        hello = hail
        hi = hail
        hey = hark
        greetings = hail and well met
        bye = fare thee well
        goodbye = fare thee well
        farewell = fare thee well
        yes = aye
        indeed = verily
        yeah = aye
        yep = aye
        no = nay
        nope = nay
        please = prithee
        sorry = I crave pardon
        perhaps = mayhap
        maybe = mayhap
        truly = verily
        assuredly = in sooth
        most = exceeding
        quickly = apace
        shortly = anon
        soon = anon
        before = ere
        ever = e'er
        never = ne'er
        over = o'er
        often = oft
        again = anew
        today = this day
        tonight = this night
        tomorrow = the morrow
        yesterday = the day past
        morning = morn
        evening = eventide
        week = sennight
        minute = moment
        second = instant
        # people and places
        man = fellow
        woman = lady
        person = soul
        persons = souls
        people = folk
        companion = good fellow
        companions = good fellows
        boss = liege
        doctor = physician
        teacher = tutor
        student = pupil
        police = the watch
        restaurant = tavern
        bar = alehouse
        shop = merchant stall
        store = merchant stall
        hotel = inn
        room = chamber
        bathroom = privy
        toilet = privy
        kitchen = scullery
        city = burgh
        town = hamlet
        country = realm
        road = thoroughfare
        residence = abode
        household = hearth
        funds = coin
        # things
        telephone = speaking-glass
        computer = thinking-engine
        laptop = thinking-engine
        internet = the great web
        email = swift missive
        message = missive
        letter = missive
        book = tome
        movie = moving picture
        music = minstrelsy
        song = ballad
        refreshment = fare
        dinner = supper
        beer = ale
        coffee = bitter brew
        clothes = raiment
        shoes = boots
        bag = satchel
        dog = hound
        horse = steed
        bird = fowl
        gift = boon
        deal = bargain
        price = sum
        expensive = dear
        cheap = paltry
        free = gratis
        # qualities
        fine = goodly
        splendid = most excellent
        magnificent = wondrous
        remarkable = marvellous
        admirable = passing fair
        unfortunate = ill
        deplorable = wretched
        agreeable = pleasant
        exquisite = fair
        handsome = comely
        ugly = foul
        peculiar = passing strange
        curious = passing strange
        considerable = great
        immense = vast
        modest = wee
        slight = wee
        weary = sore weary
        delighted = merry
        downcast = forlorn
        displeased = wroth
        apprehensive = afeard
        famished = nigh starved
        parched = athirst
        indisposed = ailing
        unhinged = moon-touched
        learned = wise
        unlettered = witless
        old = ancient
        young = youthful
        dead = perished
        # verbs
        obtain = procure
        bestow = grant
        dispatch = convey
        craft = fashion
        construct = raise up
        mend = repair
        assist = aid
        endeavour = strive
        employ = make use of
        reveal = disclose
        commence = set forth upon
        cease = desist
        tarry = bide
        converse = discourse
        inquire = beseech
        desire = covet
        require = have need of
        adore = cherish
        detest = loathe
        consider = deem
        observe = behold
        perceive = espy
        attend = hearken
        hear = hark
        depart = take leave
        reside = dwell
        eat = sup
        drink = quaff
        sleep = slumber
        walk = perambulate
        fight = do battle
        kill = slay
        die = perish
        win = prevail
        lose = forfeit
        find = discover
        read = peruse
        write = pen
        open = unfasten
        break = rend
        throw = hurl
        carry = bear
        # abstract
        difficulty = quandary
        account = tale
        position = vocation
        labour = toil
        gathering = revel
        diversion = merriment
        contest = sport
        """
    )

    private val WORDS_3 = pairs(
        """
        aye = aye, verily
        nay = nay, and thrice nay
        hail = hail and most hearty greeting
        prithee = I most humbly prithee
        mayhap = mayhap, as the fates allow
        verily = verily and in sooth
        great = passing great
        vast = most vast and boundless
        wondrous = wondrous strange and marvellous
        merry = blithe and merry
        forlorn = sore forlorn
        coin = coin of the realm
        ale = good brown ale
        hound = faithful hound
        steed = noble steed
        tome = weighty tome
        missive = sealed missive
        thinking-engine = enchanted thinking-engine
        speaking-glass = far-speaking glass
        quandary = quandary most vexing
        good fellow = fellow most true
        """
    )

    val PHRASES: Map<Int, Map<String, String>> = mapOf(1 to PHRASES_1, 2 to PHRASES_2, 3 to PHRASES_3)
    val WORDS: Map<Int, Map<String, String>> = mapOf(1 to WORDS_1, 2 to WORDS_2, 3 to WORDS_3)

    // ---------------------------------------------------------------- grammar data

    /** Base verbs we are confident enough about to conjugate. */
    val VERBS: Set<String> = words(
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
        lead leap learn leave lend let lift like listen live loathe lock long
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
        """
    )

    /** Irregular second-person-singular forms, used after "thou". */
    val IRREGULAR_2SG: Map<String, String> = pairs(
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
    val IRREGULAR_3SG: Map<String, String> = pairs(
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
    val INTERPOSED_ADVERBS: Set<String> = words(
        """
        not never ne'er ever e'er always oft often truly verily really surely
        sore most still yet but merely only also likewise then now presently
        anon thus therefore indeed exceeding
        """
    )

    /** Subjects that put the -eth ending on the verb that follows. */
    val THIRD_SINGULAR_SUBJECTS: Set<String> = words(
        """
        he she it who which one everyone someone anyone nobody everybody
        somebody anybody god fate fortune heaven lord lady king queen knight
        fellow soul man woman hound steed world sun moon night day wind sea
        heart mind life love death time truth beauty this that everything
        something nothing anything
        """
    )

    /** Subjects that never take -eth or -est. */
    val PLURAL_SUBJECTS: Set<String> = words("i we they ye you these those folk people")

    /** After one of these, a word ending in -s is almost certainly a plural noun. */
    val DETERMINERS: Set<String> = words(
        """
        the a an my thy thine your his her its our their these those some many
        few several all both each every any no two three four five ten other
        another such more most less least much of in on at for with from by
        about into over under through between betwixt among amongst
        """
    )

    /** Words ending in -s that are nouns far more often than verbs. */
    val NOT_ETH: Set<String> = words(
        """
        things matters works means times hands eyes days years ways words
        results answers questions places faces shows plays games plans forms
        lights minds changes lives loves needs wants looks feels kids notes
        marks points offers orders presents reports returns rests shares sounds
        starts states steps stops tastes trains travels trusts uses visits walks
        watches wishes wonders yields is was has does as his hers its us this
        thus always perhaps sometimes always news
        """
    )

    /** Prepositions after which "you" is an object, and so becomes "thee". */
    val PREPOSITIONS: Set<String> = words(
        """
        to for with at of from on in by about like unto upon near before after
        than between betwixt among amongst without within toward towards
        against beside behind beneath beyond ere
        """
    )

    /** Auxiliaries after which "you" is still the subject, and so stays "thou". */
    val AUXILIARIES: Set<String> = words(
        """
        do does did are were is was will would shall should can could may might
        must have has had dost doth art wilt shalt canst
        """
    )

    // ---------------------------------------------------------------- ceremony

    /** Sentence openers, added only when a sentence is finished. */
    val OPENERS: Map<Int, List<String>> = mapOf(
        1 to listOf("Indeed,", "Truly,", "I must say,"),
        2 to listOf("Verily,", "Prithee,", "Forsooth,", "Hark,", "Marry,", "In sooth,"),
        3 to listOf(
            "Hark!", "Lo!", "By my troth,", "Forsooth and verily,",
            "Attend me now,", "Let it be known throughout the land,"
        )
    )

    /** Sentence closers, added only when a sentence is finished. */
    val CLOSERS: Map<Int, List<String>> = mapOf(
        1 to listOf(", to be sure", ", I should think"),
        2 to listOf(", i' faith", ", by my troth", ", I do declare", ", and there an end"),
        3 to listOf(
            ", and may the heavens bear witness", ", as I live and breathe",
            ", upon mine honour", ", and so it shall be recorded",
            ", though the four winds contend against it"
        )
    )

    /** Faux-antique respellings, offered at the Sovereign tier. */
    val OLDE_SPELLINGS: Map<String, String> = pairs(
        """
        old = olde
        ancient = auncient
        shop = shoppe
        magic = magick
        music = musick
        public = publick
        logic = logick
        good = goode
        done = donne
        gone = gonne
        tale = tayle
        night = nyght
        light = lyght
        right = ryght
        quite = quyte
        town = towne
        word = worde
        world = worlde
        friend = friende
        house = housse
        book = booke
        king = kyng
        wine = wyne
        """
    )
}
