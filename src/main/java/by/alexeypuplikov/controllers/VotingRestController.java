package by.alexeypuplikov.controllers;

import by.alexeypuplikov.models.Vote;
import by.alexeypuplikov.models.Voting;
import by.alexeypuplikov.models.VotingOption;
import by.alexeypuplikov.repositories.UserRepository;
import by.alexeypuplikov.repositories.VoteRepository;
import by.alexeypuplikov.repositories.VotingOptionRepository;
import by.alexeypuplikov.repositories.VotingRepository;
import by.alexeypuplikov.utils.ValidateVoting;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/voting")
public class VotingRestController {
    private final VotingRepository votingRepository;
    private final VotingOptionRepository votingOptionRepository;
    private final VoteRepository voteRepository;
    private final UserRepository userRepository;

    @Autowired
    public VotingRestController(VotingRepository votingRepository, VotingOptionRepository votingOptionRepository, VoteRepository voteRepository, UserRepository userRepository) {
        this.votingRepository = votingRepository;
        this.votingOptionRepository = votingOptionRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Collection<Voting>> getAllVoting() {
        return new ResponseEntity<>((Collection<Voting>) votingRepository.findAll(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> addVoting(@RequestBody Voting voting) {
        ValidateVoting.validateAddTopic(votingRepository, voting.getTopic());
        return new ResponseEntity<>(votingRepository.save(voting), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{topic}/addVotingOption")
    public ResponseEntity<?> addVotingOption(@PathVariable String topic, @RequestBody VotingOption votingOption) {
        Voting voting = votingRepository.findByTopic(topic);
        ValidateVoting.validateVotingOption(votingOptionRepository, votingOption.getOptionText(), voting);
        votingOption.setVoting(voting);
        return new ResponseEntity<>(votingOptionRepository.save(votingOption), HttpStatus.CREATED);
    }

    @RequestMapping(method = RequestMethod.POST, params = {"topic", "changeState"})
    public ResponseEntity<?> changeVotingState(@RequestParam(value = "topic") String topic, @RequestParam(value = "changeState") String changeState, HttpServletRequest request) {
        Voting voting = votingRepository.findByTopic(topic);
        ValidateVoting.validateLaunchVoting(voting, topic, changeState);
        if (changeState.equals("launch")) {
            voting.setLink(String.valueOf(this.getURLValue(request)) + "/" + voting.getTopic().replace(" ", "%20"));
            voting.setLaunched(true);
        }
        if (changeState.equals("close")) {
            voting.setLink(null);
            voting.setLaunched(false);
        }
        return new ResponseEntity<>(votingRepository.save(voting), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/{topic}", params = {"optionText", "login"})
    public ResponseEntity<?> registrationVote(@PathVariable String topic, @RequestParam(value = "optionText") String optionText, @RequestParam(value = "login") String login) {
        Voting voting = votingRepository.findByTopic(topic);
        ValidateVoting.validateRegistrationVote(votingOptionRepository, optionText, voting, login, userRepository, voteRepository);
        VotingOption votingOption = votingOptionRepository.findByVotingAndOptionText(voting, optionText);
        Vote vote = new Vote(votingOption, voting, userRepository.findByLogin(login));
        return new ResponseEntity<>(voteRepository.save(vote), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{topic}")
    public ResponseEntity<?> getVoting(@PathVariable String topic) {
        Voting voting = votingRepository.findByTopic(topic);
        return new ResponseEntity<>(voting, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{topic}/statistics")
    public ResponseEntity<?> findStatistics(@PathVariable String topic) {
        Map<String, Long> statistics = new HashMap<>();
        Voting voting = votingRepository.findByTopic(topic);
        List<VotingOption> votingOptions = votingOptionRepository.findByVoting(voting);
        for (VotingOption votingOption : votingOptions) {
            statistics.put(votingOption.getOptionText(), voteRepository.countByVotingAndVotingOption(voting, votingOption));
        }
        return new ResponseEntity<>(statistics, HttpStatus.OK);
    }

    @RequestMapping(value = "/", produces = "application/json")
    public StringBuffer getURLValue(HttpServletRequest request) {
        return request.getRequestURL();
    }
}
