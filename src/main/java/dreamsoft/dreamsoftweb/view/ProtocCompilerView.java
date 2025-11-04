package dreamsoft.dreamsoftweb.view;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/proto")
public class ProtocCompilerView {

    @GetMapping()
    public String index() {
        return "proto-index";
    }
}
